package com.spendingbuddy.app.p2p

import com.spendingbuddy.app.*
import org.junit.Assert.*
import org.junit.Test

class SyncProtocolTest {
    private val A = "aaa"; private val B = "bbb"; private val C = "ccc"
    private val group = Group("g1", "Crew", "gsecret", 1, adminId = "aaa")
    private fun circle(selfId: String, name: String = "Self", ids: List<String> = listOf(A, B, C)) = BuddyState(
        name = name, selfId = selfId, group = group,
        members = ids.map { Member(it, if (it == selfId) name else "M$it", joinedAt = 1) }
    )
    private fun sync(vararg e: PeerEvent, from: String = B, members: List<Member> = emptyList()) =
        SyncMessage(type = "sync", sender = "S", groupId = "g1", events = e.toList(), members = members)
    private fun rejects(m: SyncMessage) = assertThrows(IllegalArgumentException::class.java) { validateMessage(m) }

    // ---- join code ----
    private fun payload(expires: Long = 10_000) = JoinCodePayload("g1", "Crew", A, "ticket-abc", "0123456789abcdef", expires)

    @Test fun joinCodeRoundTripsAndToleratesShareText() {
        val code = encodeJoinCode(payload())
        assertTrue(code.startsWith("pc1."))
        val parsed = parseJoinCode("Join my circle:\n  $code  \nthanks", 1_000)
        assertEquals(payload(), parsed.payload); assertFalse(parsed.expired)
        assertTrue(parseJoinCode(code, 10_000).expired)
    }

    @Test fun joinCodeTamperTruncationAndHostileInputRejected() {
        val code = encodeJoinCode(payload())
        fun bad(s: String, now: Long = 1_000) = assertThrows(IllegalArgumentException::class.java) { parseJoinCode(s, now) }
        bad(code.substring(0, 20) + (if (code[20] == 'x') "y" else "x") + code.substring(21))
        bad(code.dropLast(3)); bad("pc1."); bad("pc1.!!!"); bad("sb1." + code.removePrefix("pc1.")); bad("")
        bad("pc1." + "A".repeat(MAX_JOIN_CODE_LENGTH))
        bad(code, now = 10_000 - MAX_JOIN_CODE_LIFETIME_MS - 1)
        bad(encodeJoinCode(payload().copy(nonce = "short")))
        bad(encodeJoinCode(payload().copy(groupId = "bad id!")))
        bad(encodeJoinCode(payload().copy(adminId = "")))
        bad(encodeJoinCode(payload().copy(groupName = "")))
        bad(encodeJoinCode(payload().copy(ticket = "t".repeat(MAX_TICKET_LENGTH + 1))))
        bad(encodeJoinCode(payload().copy(expiresAt = 0)))
    }

    // ---- join request + admin approval ----
    private fun adminWithCode(now: Long = 0): Pair<BuddyState, JoinCodePayload> {
        val (s, code) = issueJoinCode(circle(A, "Asha", listOf(A)), "tkt", now)
        return s to parseJoinCode(code, now + 1).payload
    }
    private fun req(id: String, p: JoinCodePayload, name: String = "Dev", now: Long = 5) =
        decodeMessage(encodeMessage(buildJoinRequest(BuddyState(name = name, selfId = id), p, now = now)))

    @Test fun codeCarriesNoSecretAndOnlyAdminCanIssue() {
        val (s, p) = adminWithCode()
        assertEquals(A, p.adminId); assertEquals(JOIN_CODE_TTL_MS, p.expiresAt)
        assertFalse(encodeJoinCode(p).contains("gsecret"))
        assertEquals(s.joinNonce, p.nonce)
        assertThrows(IllegalArgumentException::class.java) { issueJoinCode(circle(B), "t", 0) }
        assertThrows(IllegalArgumentException::class.java) { rotateJoinCode(circle(B), 0) }
        // same live code is reused, then replaced once expired
        assertEquals(p.nonce, parseJoinCode(issueJoinCode(s, "tkt", 100).second, 100).payload.nonce)
        assertNotEquals(p.nonce, parseJoinCode(issueJoinCode(s, "tkt", JOIN_CODE_TTL_MS + 1).second, JOIN_CODE_TTL_MS + 1).payload.nonce)
    }

    @Test fun requestIsQueuedNotJoinedAndJoinerGetsNoData() {
        val (admin, p) = adminWithCode()
        val out = handleJoinRequest(admin, "ddd", req("ddd", p), 5)
        assertEquals("join_pending", out.reply.type)
        assertNotNull(out.request); assertEquals(1, out.state.pendingJoins.size)
        assertNull(out.reply.snapshot); assertTrue(out.reply.members.isEmpty()); assertNull(out.reply.group)
        assertFalse(isActiveMember(out.state, "ddd"))
        assertTrue("pending phone is not a valid sender", fanOut(out.state).none { it.member.endpointId == "ddd" })
        assertThrows(IllegalArgumentException::class.java) { applyIncoming(out.state, "ddd", sync(from = "ddd"), 6) }
        val again = handleJoinRequest(out.state, "ddd", req("ddd", p), 6)
        assertEquals("join_pending", again.reply.type); assertNull(again.request); assertEquals(1, again.state.pendingJoins.size)
        val joiner = BuddyState(name = "Dev", selfId = "ddd")
        assertTrue(applyJoinReply(joiner, A, again.reply, p) is JoinReply.Pending)
    }

    @Test fun approvalAddsMemberAndJoinerReceivesGroup() {
        val (admin0, p) = adminWithCode()
        val admin = admin0.copy(members = circle(A).members)
        val q = handleJoinRequest(admin, "ddd", req("ddd", p), 5)
        val approved = approveJoin(q.state, q.request!!.id, 7)
        assertTrue(isActiveMember(approved.state, "ddd")); assertTrue(approved.state.pendingJoins.isEmpty())
        val joiner = BuddyState(name = "Dev", selfId = "ddd")
        val r = applyJoinReply(joiner, A, decodeMessage(encodeMessage(approved.joinOk)), p, 8) as JoinReply.Joined
        assertEquals("g1", r.state.group?.id); assertEquals(A, r.state.group?.adminId)
        assertEquals(setOf(A, B, C, "ddd"), r.state.members.map { it.endpointId }.toSet())
        assertEquals(setOf(A), r.state.friends.keys)
        // approved phone re-polling gets join_ok; other members learn via membership merge
        assertEquals("join_ok", handleJoinRequest(approved.state, "ddd", req("ddd", p), 9).reply.type)
        val b = applyIncoming(circle(B), A, fanOut(approved.state).first { it.member.endpointId == B }.message.copy(groupId = "g1"), 10).first
        assertTrue(isActiveMember(b, "ddd"))
    }

    @Test fun nonAdminCannotApproveOrTakeRequests() {
        val (admin, p) = adminWithCode()
        val q = handleJoinRequest(admin, "ddd", req("ddd", p), 5)
        val asMember = circle(B).copy(pendingJoins = q.state.pendingJoins) // tampered local state, still not admin
        assertThrows(IllegalArgumentException::class.java) { approveJoin(asMember, q.request!!.id, 6) }
        assertThrows(IllegalArgumentException::class.java) { declineJoin(asMember, q.request!!.id) }
        assertEquals("error", handleJoinRequest(circle(B), "ddd", req("ddd", p), 5).reply.type)
        assertThrows(IllegalArgumentException::class.java) { approveJoin(q.state, "no-such-request", 6) }
    }

    @Test fun declineBlocksAndExpiredOrRotatedOrWrongCodeRefused() {
        val (admin, p) = adminWithCode()
        val q = handleJoinRequest(admin, "ddd", req("ddd", p), 5)
        val declined = declineJoin(q.state, q.request!!.id)
        assertTrue(declined.pendingJoins.isEmpty())
        assertEquals("error", handleJoinRequest(declined, "ddd", req("ddd", p), 6).reply.type)
        val joiner = BuddyState(name = "Dev", selfId = "ddd")
        assertTrue(applyJoinReply(joiner, A, handleJoinRequest(declined, "ddd", req("ddd", p), 6).reply, p) is JoinReply.Refused)
        // expired code
        assertEquals("error", handleJoinRequest(admin, "eee", req("eee", p), p.expiresAt + 1).reply.type)
        // rotated code: old nonce no longer works, new one does
        val rotated = rotateJoinCode(admin, 100)
        assertEquals("error", handleJoinRequest(rotated, "eee", req("eee", p), 101).reply.type)
        val fresh = parseJoinCode(issueJoinCode(rotated, "tkt", 101).second, 101).payload
        assertEquals("join_pending", handleJoinRequest(rotated, "eee", req("eee", fresh), 101).reply.type)
        // pending requests survive rotation
        val keep = handleJoinRequest(admin, "fff", req("fff", p), 5).state
        assertEquals(1, rotateJoinCode(keep, 6).pendingJoins.size)
        // wrong group / forged member entry
        assertEquals("error", handleJoinRequest(admin, "eee", req("eee", p).copy(groupId = "other"), 5).reply.type)
        assertEquals("error", handleJoinRequest(admin, "zzz", req("eee", p), 5).reply.type)
    }

    @Test fun requestSpamIsCappedAndDoesNotBurnTheCode() {
        val (admin, p) = adminWithCode()
        var s = admin
        for (i in 0 until MAX_PENDING_JOINS) s = handleJoinRequest(s, "x$i", req("x$i", p), 5).state
        assertEquals(MAX_PENDING_JOINS, s.pendingJoins.size)
        val over = handleJoinRequest(s, "late", req("late", p), 6)
        assertEquals("error", over.reply.type); assertEquals(MAX_PENDING_JOINS, over.state.pendingJoins.size)
        for (i in 0 until 50) s = handleJoinRequest(s, "spam$i", req("spam$i", p), 6).state
        assertEquals(MAX_PENDING_JOINS, s.pendingJoins.size)
        assertEquals(p.nonce, s.joinNonce) // code still valid
        assertEquals("join_pending", handleJoinRequest(declineJoin(s, s.pendingJoins.first().id), "late", req("late", p), 7).reply.type)
        // pending requests lapse after 24h, freeing the queue
        assertTrue(purgeExpiredJoins(s, 5 + PENDING_JOIN_TTL_MS).pendingJoins.isEmpty())
    }

    @Test fun approvalRespectsCapAndTombstones() {
        val (admin, p) = adminWithCode()
        val q = handleJoinRequest(admin, "ddd", req("ddd", p), 5)
        val full = q.state.copy(members = List(MAX_MEMBERS) { Member(if (it == 0) A else "m$it", "M$it", joinedAt = 1) })
        assertThrows(IllegalArgumentException::class.java) { approveJoin(full, q.request!!.id, 6) }
        val tomb = admin.copy(members = admin.members + Member("ddd", "Dev", status = MEMBER_LEFT, leftAt = 2))
        assertEquals("error", handleJoinRequest(tomb, "ddd", req("ddd", p), 5).reply.type)
        assertThrows(IllegalArgumentException::class.java) { approveJoin(q.state, q.request!!.id, 5 + PENDING_JOIN_TTL_MS) } // lapsed
    }

    @Test fun applyJoinReplyChecksOriginGroupAndErrors() {
        val (admin, p) = adminWithCode()
        val q = handleJoinRequest(admin, "ddd", req("ddd", p), 5)
        val ok = approveJoin(q.state, q.request!!.id, 6).joinOk
        val joiner = BuddyState(name = "Dev", selfId = "ddd")
        assertTrue(applyJoinReply(joiner, "evil", ok, p) is JoinReply.Refused)
        assertTrue(applyJoinReply(joiner, A, ok.copy(groupId = "x", group = ok.group!!.copy(id = "x")), p) is JoinReply.Refused)
        assertTrue(applyJoinReply(joiner, A, SyncMessage(type = "error", error = "nope"), p) is JoinReply.Refused)
        assertTrue(applyJoinReply(circle("ddd"), A, ok, p) is JoinReply.Refused)
        assertTrue(applyJoinReply(joiner, A, ok.copy(members = listOf(Member(A, "Asha", status = MEMBER_LEFT))), p) is JoinReply.Refused)
        assertTrue(applyJoinReply(joiner, A, ok.copy(v = 1), p) is JoinReply.Refused)
    }

    @Test fun createGroupMakesCreatorAdminAndLeaveClearsGroupState() {
        val s = createGroup(BuddyState(name = "Asha", selfId = A), "  Crew ")
        assertEquals("Crew", s.group?.name); assertEquals(A, s.group?.adminId); assertTrue(isAdmin(s))
        assertThrows(IllegalArgumentException::class.java) { createGroup(s, "Again") }
        assertThrows(IllegalArgumentException::class.java) { createGroup(BuddyState(selfId = A), "") }
        val withPeers = s.copy(members = s.members + Member(B, "Ravi", joinedAt = 9) + Member(C, "Cy", joinedAt = 5), expenses = listOf(Expense(id = "x", amountPaise = 5)))
        val left = leaveGroup(withPeers, now = 50)
        assertNull(left.state.group); assertTrue(left.state.members.isEmpty()); assertEquals(1, left.state.expenses.size)
        assertEquals(setOf(B, C), left.notices.map { it.member.endpointId }.toSet())
        left.notices.forEach { validateMessage(it.message) }
        assertNotNull(left.broadcast)
    }

    // ---- membership merge ----
    @Test fun mergeUnionDedupAndNameAuthority() {
        val local = listOf(Member(A, "Me"), Member(B, "Bee"))
        val merged = mergeMembers(local, listOf(Member(B, "Bee2"), Member(C, "Cee"), Member(C, "Cee")), fromId = C, selfId = A)
        assertEquals(listOf(A, B, C), merged.map { it.endpointId })
        assertEquals("Bee", merged.first { it.endpointId == B }.displayName) // only B may rename B
        val renamed = mergeMembers(merged, listOf(Member(B, "Bee2")), fromId = B, selfId = A)
        assertEquals("Bee2", renamed.first { it.endpointId == B }.displayName)
        assertEquals("Me", mergeMembers(local, listOf(Member(A, "Hacked")), fromId = B, selfId = A).first().displayName)
    }

    @Test fun mergeTombstonesOnlyFromTheLeaverAndCannotBeResurrected() {
        val local = listOf(Member(A, "Me"), Member(B, "Bee"))
        val gone = mergeMembers(local, listOf(Member(B, "Bee", status = MEMBER_LEFT, leftAt = 9)), B, A)
        assertFalse(gone.first { it.endpointId == B }.active)
        assertTrue("nobody can remove another member", mergeMembers(local, listOf(Member(B, "Bee", status = MEMBER_LEFT, leftAt = 9)), C, A).first { it.endpointId == B }.active)
        assertTrue("no pre-emptive tombstones", mergeMembers(local, listOf(Member(C, "Cee", status = MEMBER_LEFT)), B, A).none { it.endpointId == C })
        assertFalse(mergeMembers(local, listOf(Member(C, "Cee", status = MEMBER_LEFT)), C, A).first { it.endpointId == C }.active)
        val back = mergeMembers(gone, listOf(Member(B, "Bee")), B, A)
        assertFalse(back.first { it.endpointId == B }.active)
        assertTrue("others cannot tombstone self", mergeMembers(local, listOf(Member(A, "Me", status = MEMBER_LEFT)), B, A).first().active)
    }

    @Test fun mergeLowersJoinedAtToConverge() {
        val m = mergeMembers(listOf(Member(B, "B", joinedAt = 9)), listOf(Member(B, "B", joinedAt = 4)), C, A)
        assertEquals(4L, m.single().joinedAt)
        assertEquals(4L, mergeMembers(m, listOf(Member(B, "B", joinedAt = 8)), C, A).single().joinedAt)
    }

    @Test fun mergeEnforcesCapAndValidatesNames() {
        val full = List(MAX_MEMBERS) { Member("m$it", "N$it") }
        val merged = mergeMembers(full, listOf(Member("extra", "Extra"), Member("m1", "N1")), "m1", "m0")
        assertEquals(MAX_MEMBERS, merged.size)
        assertFalse(merged.any { it.endpointId == "extra" })
        val tomb = mergeMembers(full, listOf(Member("t", "T", status = MEMBER_LEFT)), "t", "m0")
        assertEquals(MAX_MEMBERS + 1, tomb.size) // tombstones do not count toward the cap
        val hostile = listOf(Member("x1", ""), Member("x2", " pad "), Member("x3", "ctl\u0007"), Member("x4", "n".repeat(MAX_DISPLAY_NAME + 1)), Member("bad id", "Ok"))
        assertTrue(mergeMembers(listOf(Member("m0", "Me")), hostile, "m0", "m0").size == 1)
    }

    @Test fun membershipPropagatesThroughSync() {
        val a = circle(A, ids = listOf(A, B))
        val msg = sync(from = B, members = listOf(Member(B, "Bee"), Member(C, "Cee")))
        val next = applyIncoming(a, B, msg.copy(groupId = "g1"), 5).first
        assertTrue(isActiveMember(next, C))
        assertTrue("c is now a valid sender", isSenderAllowed("sync", C, next))
    }

    // ---- sender / group gating ----
    @Test fun senderMustBeActiveMember() {
        val a = circle(A)
        assertTrue(isSenderAllowed("sync", B, a))
        assertFalse(isSenderAllowed("sync", "stranger", a))
        assertFalse(isSenderAllowed("sync", A, a)) // our own id
        assertTrue(isSenderAllowed("join_request", "stranger", a)) // gated by admin approval instead
        assertFalse(isSenderAllowed("sync", B, a.copy(members = a.members.map { if (it.endpointId == B) it.copy(status = MEMBER_LEFT) else it })))
        assertFalse(isSenderAllowed("sync", B, BuddyState(selfId = A)))
        assertThrows(IllegalArgumentException::class.java) { applyIncoming(a, "stranger", sync(), 1) }
        assertThrows(IllegalArgumentException::class.java) { applyIncoming(a, B, sync().copy(groupId = "other"), 1) }
        assertThrows(IllegalArgumentException::class.java) { applyIncoming(a, B, SyncMessage(type = "join_request", groupId = "g1", joinNonce = "x", members = listOf(Member(B, "b"))), 1) }
        assertThrows(IllegalArgumentException::class.java) { applyIncoming(BuddyState(selfId = A), B, sync(), 1) }
    }

    // ---- snapshots, events, acks ----
    @Test fun perMemberSnapshotsUseRevisionOrdering() {
        val a = circle(A).copy(friends = mapOf(B to FriendSnapshot(name = "B", revision = 5, receivedAt = 1)))
        val stale = sync().copy(snapshot = WireSnapshot(name = "B", revision = 4))
        assertEquals(5L, applyIncoming(a, B, stale, 10).first.friends[B]?.revision)
        val fresh = applyIncoming(a, C, sync(from = C).copy(snapshot = WireSnapshot(name = "C", revision = 1, dailyLimitPaise = 700)), 10).first
        assertEquals(700L, fresh.friends[C]?.dailyLimitPaise)
        assertEquals(5L, fresh.friends[B]?.revision)
    }

    @Test fun eventsDedupedPerSenderAndTargetedToSelf() {
        val a = circle(A)
        val nudge = PeerEvent(id = "n1", type = "nudge", minutes = 30, createdAt = 100, target = A)
        val once = applyIncoming(a, B, sync(nudge), 500).first
        val twice = applyIncoming(once, B, sync(nudge), 600)
        assertEquals(1, twice.first.seenEventIds[B]!!.count { it == "n1" })
        assertTrue(twice.second.isEmpty())
        assertEquals(B, once.incomingNudge?.senderId); assertEquals(500L + 30 * 60_000L, once.incomingNudge?.endsAt)
        // same id from a different sender is a separate event
        assertEquals(C, applyIncoming(once, C, sync(nudge, from = C), 700).first.incomingNudge?.senderId)
        // a nudge addressed to someone else is not shown here
        val other = applyIncoming(a, B, sync(nudge.copy(id = "n2", target = C)), 500).first
        assertNull(other.incomingNudge)
    }

    @Test fun acksAreScopedToTheAcknowledgingMember() {
        val toB = PeerEvent(id = "e1", type = "nudge", minutes = 30, target = B)
        val toC = PeerEvent(id = "e1", type = "nudge", minutes = 30, target = C)
        val a = circle(A).copy(outbox = listOf(toB, toC), outgoingNudges = listOf(OutgoingNudge("e1", 30, 1, 0, B), OutgoingNudge("e1", 30, 1, 0, C)))
        val next = applyIncoming(a, B, SyncMessage(type = "sync", groupId = "g1", acks = listOf("e1")), 99).first
        assertEquals(listOf(C), next.outbox.map { it.target })
        assertEquals(99L, next.outgoingNudges.first { it.targetId == B }.deliveredAt)
        assertEquals(0L, next.outgoingNudges.first { it.targetId == C }.deliveredAt)
    }

    // ---- suggestions ----
    @Test fun suggestionOnlyAcceptedByTarget() {
        var b = circle(B)
        var a = circle(A)
        a = queueSuggestion(a, B, 100, 700, 10)
        val ev = a.outbox.single()
        assertEquals(B, ev.target)
        // C also receives a copy addressed to B: must be ignored
        val c = circle(C)
        assertTrue(applyIncoming(c, A, sync(ev, from = A), 11).first.suggestions.isEmpty())
        // B receives it
        b = applyIncoming(b, A, sync(ev, from = A), 11).first
        val sug = b.suggestions.single()
        assertEquals(A, sug.senderId); assertEquals(B, sug.targetId)
        // a non-target cannot respond, even if they somehow hold the suggestion
        assertNull(respondToSuggestion(b.copy(selfId = C), sug.id, true, 12))
        assertNull(respondToSuggestion(b, "unknown", true, 12))
        val accepted = respondToSuggestion(b, sug.id, true, 12)!!
        assertEquals(100L, accepted.dailyLimitPaise); assertEquals(700L, accepted.weeklyLimitPaise)
        assertEquals(b.revision + 1, accepted.revision)
        assertNull("already answered", respondToSuggestion(accepted, sug.id, false, 13))
        val declined = respondToSuggestion(b, sug.id, false, 12)!!
        assertEquals(b.dailyLimitPaise, declined.dailyLimitPaise)
        // the reply is only honoured from B, not from C
        val reply = accepted.outbox.single { it.type == "suggestion_reply" }
        assertEquals(A, reply.target)
        val forged = applyIncoming(a, C, sync(reply.copy(id = "forged"), from = C), 14).first
        assertEquals("pending", forged.sentSuggestions.single().status)
        val real = applyIncoming(a, B, sync(reply), 14).first
        assertEquals("accepted", real.sentSuggestions.single().status)
        // a suggestion cannot be addressed to self or a non-member
        assertThrows(IllegalArgumentException::class.java) { queueSuggestion(a, A, 1, 1) }
        assertThrows(IllegalArgumentException::class.java) { queueSuggestion(a, "zzz", 1, 1) }
    }

    @Test fun nudgeQueueTargetsOneMemberAndValidates() {
        val a = queueNudge(circle(A), C, 30, 5)
        assertEquals(C, a.outbox.single().target); assertEquals(C, a.outgoingNudges.single().targetId)
        assertThrows(IllegalArgumentException::class.java) { queueNudge(circle(A), "zzz", 30) }
        assertThrows(IllegalArgumentException::class.java) { queueNudge(circle(A), B, 0) }
    }

    @Test fun nudgeLimitedToOnePerTargetPerKolkataDay() {
        val noon = java.time.ZonedDateTime.of(2026, 11, 9, 12, 0, 0, 0, INDIA_ZONE).toInstant().toEpochMilli()
        val a = queueNudge(circle(A), B, 30, noon)
        assertFalse(canNudge(a, B, noon + 60_000))
        assertThrows(IllegalArgumentException::class.java) { queueNudge(a, B, 30, noon + 60_000) }
        assertTrue("another member is separate", canNudge(a, C, noon + 60_000))
        assertTrue("next IST day", canNudge(a, B, noon + 13 * 3_600_000L))
        assertFalse("self and strangers", canNudge(a, A, noon) || canNudge(a, "zzz", noon))
        // 23:30 IST and 00:30 IST are different days even though only an hour apart
        val late = java.time.ZonedDateTime.of(2026, 11, 9, 23, 30, 0, 0, INDIA_ZONE).toInstant().toEpochMilli()
        assertTrue(canNudge(queueNudge(circle(A), B, 30, late), B, late + 3_600_000L))
    }

    @Test fun receiverAcceptsOneNudgePerSenderPerDayAndSuppressesAfterCheckIn() {
        val noon = java.time.ZonedDateTime.of(2026, 11, 9, 12, 0, 0, 0, INDIA_ZONE).toInstant().toEpochMilli()
        fun nudge(id: String) = PeerEvent(id = id, type = "nudge", minutes = 30, createdAt = noon - 1_000, target = A)
        val first = applyIncoming(circle(A), B, sync(nudge("n1")), noon).first
        assertEquals("n1", first.incomingNudge?.id)
        val second = applyIncoming(first, B, sync(nudge("n2")), noon + 1_000).first
        assertEquals("n1", second.incomingNudge?.id)
        val tomorrow = noon + 24 * 3_600_000L
        assertEquals("n3", applyIncoming(second, B, sync(nudge("n3").copy(createdAt = tomorrow - 1_000)), tomorrow).first.incomingNudge?.id)
        assertNull("stale (older than 12h) nudges are dropped", applyIncoming(circle(A), B, sync(nudge("n5")), tomorrow).first.incomingNudge)
        // already checked in today: a late nudge is dropped; a pending one stops being active
        val done = circle(A).copy(checkIns = listOf(CheckIn(istDate(noon), noon)))
        val (afterDone, notices) = applyIncoming(done, B, sync(nudge("n4")), noon)
        assertNull(afterDone.incomingNudge); assertTrue(notices.isEmpty())
        assertNotNull(activeNudge(first, noon + 1_000))
        assertNull(activeNudge(first.copy(checkIns = done.checkIns), noon + 1_000))
        assertNull("expired", activeNudge(first, noon + 31 * 60_000L))
    }

    @Test fun leaveEventRemovesMemberAndTheirState() {
        val a = queueNudge(circle(A), B, 30).copy(friends = mapOf(B to FriendSnapshot(name = "B")), suggestions = listOf(BudgetSuggestion("s", 1, 1, senderId = B, targetId = A)))
        val next = applyIncoming(a, B, sync(PeerEvent(id = "l", type = "leave", target = A)), 40).first
        assertFalse(isActiveMember(next, B))
        assertNull(next.friends[B]); assertTrue(next.outbox.isEmpty()); assertTrue(next.suggestions.isEmpty())
        assertThrows(IllegalArgumentException::class.java) { applyIncoming(next, B, sync(), 41) } // no longer a member
        assertTrue(isActiveMember(next, C))
    }

    // ---- fan-out ----
    @Test fun fanOutBuildsOneMessagePerOtherActiveMember() {
        val base = circle(A, "Asha")
        val s = queueNudge(queueSuggestion(base, B, 1, 7), C, 15).copy(
            seenEventIds = mapOf(B to listOf("b1"), C to listOf("c1")),
            members = base.members.map { if (it.endpointId == C) it.copy(status = MEMBER_LEFT) else it } + Member("ddd", "Dev"))
        val items = fanOut(s, 1)
        assertEquals(setOf(B, "ddd"), items.map { it.member.endpointId }.toSet())
        val toB = items.first { it.member.endpointId == B }.message
        assertEquals(listOf("budget_suggestion"), toB.events.map { it.type })
        assertEquals(listOf("b1"), toB.acks)
        assertTrue(items.first { it.member.endpointId == "ddd" }.message.events.isEmpty())
        items.forEach { assertEquals("g1", it.message.groupId); validateMessage(decodeMessage(encodeMessage(it.message))) }
        assertTrue(toB.members.any { it.endpointId == C && !it.active })
        assertTrue(fanOut(BuddyState(), 1).isEmpty())
    }

    @Test fun fanOutBoundsEventsAndAcks() {
        val s = circle(A).copy(outbox = List(400) { PeerEvent(id = "o$it", type = "nudge", minutes = 5, target = B) },
            seenEventIds = mapOf(B to List(2_000) { "id$it" }))
        val m = fanOut(s).first { it.member.endpointId == B }.message
        assertEquals(MAX_EVENTS_PER_MESSAGE, m.events.size); assertEquals(MAX_ACKS_PER_MESSAGE, m.acks.size); assertEquals("id1999", m.acks.last())
        validateMessage(m)
        var st = circle(A)
        for (i in 0 until 1_200) st = applyIncoming(st, B, sync(PeerEvent(id = "n$i", type = "nudge", minutes = 5, target = A)), 1).first
        assertEquals(MAX_SEEN_EVENT_IDS, st.seenEventIds[B]!!.size)
        assertEquals("n1199", st.seenEventIds[B]!!.last())
    }

    // ---- wire validation / hostile input ----
    @Test fun messageRoundTripAndSizeLimit() {
        val m = SyncMessage(type = "sync", sender = "Asha", groupId = "g1", snapshot = WireSnapshot(name = "Asha", revision = 4),
            events = listOf(PeerEvent(type = "nudge", minutes = 30, target = B)), acks = listOf("x"), members = listOf(Member(A, "Asha")))
        assertEquals(m, decodeMessage(encodeMessage(m)))
        assertThrows(IllegalArgumentException::class.java) { decodeMessage(ByteArray(MAX_MESSAGE_BYTES + 1)) }
    }

    @Test fun versionAndTypeRejected() {
        rejects(SyncMessage(v = 1, type = "sync")); rejects(SyncMessage(v = 3, type = "sync")); rejects(SyncMessage(v = 0, type = "sync"))
        rejects(SyncMessage(type = "bogus")); rejects(SyncMessage(type = "pair")); rejects(SyncMessage(type = "pair_ok"))
        assertThrows(IllegalArgumentException::class.java) { decodeMessage(encodeMessage(SyncMessage(v = 99, type = "sync"))) }
        assertThrows(IllegalArgumentException::class.java) { applyIncoming(circle(A), B, SyncMessage(v = 1, type = "sync", groupId = "g1"), 1) }
        assertNotEquals("spendingbuddy/sync/1", SYNC_ALPN_TEXT)
    }

    @Test fun oversizedAndHostileFieldsRejected() {
        rejects(SyncMessage(type = "sync", events = List(MAX_EVENTS_PER_MESSAGE + 1) { PeerEvent(id = "e$it", type = "leave", target = B) }))
        rejects(SyncMessage(type = "sync", acks = List(MAX_ACKS_PER_MESSAGE + 1) { "a$it" }))
        rejects(SyncMessage(type = "sync", acks = listOf("x".repeat(MAX_ID_LENGTH + 1))))
        rejects(SyncMessage(type = "sync", sender = "x".repeat(MAX_NAME_LENGTH + 1)))
        rejects(SyncMessage(type = "sync", groupId = "x".repeat(MAX_ID_LENGTH + 1)))
        rejects(SyncMessage(type = "join_request", groupId = "g1", joinNonce = "x".repeat(1000), members = listOf(Member(B, "b"))))
        rejects(SyncMessage(type = "join_request", groupId = "g1", members = listOf(Member(B, "b")))) // no nonce
        rejects(SyncMessage(type = "join_request", groupId = "g1", joinNonce = "s", members = emptyList()))
        rejects(SyncMessage(type = "join_request", groupId = "g1", joinNonce = "s", members = listOf(Member(B, "b"), Member(C, "c"))))
        rejects(SyncMessage(type = "join")) // old type
        rejects(SyncMessage(type = "sync", adminId = "bad id")); rejects(SyncMessage(type = "sync", adminEpoch = -1))
        rejects(SyncMessage(type = "sync", senderId = "bad id")); rejects(SyncMessage(type = "sync", sig = "s".repeat(MAX_SIGNATURE_LENGTH + 1)))
        rejects(SyncMessage(type = "sync", snapshot = WireSnapshot(expenses = listOf(Expense(id = "a", amountPaise = 1))))) // totals-only must not carry records
        rejects(SyncMessage(type = "sync", snapshot = WireSnapshot(streak = -1)))
        rejects(SyncMessage(type = "sync", snapshot = WireSnapshot(dailySpentPaise = -1)))
        rejects(SyncMessage(type = "join_ok", groupId = "g1", members = listOf(Member(A, "a")))) // no group
        rejects(SyncMessage(type = "sync", members = List(MAX_MEMBERS + MAX_TOMBSTONES + 1) { Member("m$it", "n") }))
        rejects(SyncMessage(type = "sync", members = listOf(Member(A, "a"), Member(A, "a2"))))
        rejects(SyncMessage(type = "sync", members = listOf(Member(A, "a", directAddresses = List(MAX_ADDRESSES + 1) { "1.1.1.1:$it" }))))
        rejects(SyncMessage(type = "sync", members = listOf(Member(A, "a", status = "admin"))))
        rejects(SyncMessage(type = "sync", members = listOf(Member("bad id", "a"))))
        rejects(SyncMessage(type = "sync", members = listOf(Member(A, "x\u0000y"))))
        rejects(SyncMessage(type = "join_ok", groupId = "g1", group = Group("g1", "", "s"), members = listOf(Member(A, "a"))))
        rejects(sync(PeerEvent(id = "", type = "leave", target = B)))
        rejects(sync(PeerEvent(id = "y".repeat(MAX_ID_LENGTH + 1), type = "leave", target = B)))
        rejects(sync(PeerEvent(id = "e", type = "unlink", target = B)))
        rejects(sync(PeerEvent(id = "e", type = "nudge", minutes = 5, target = "")))
        rejects(sync(PeerEvent(id = "e", type = "nudge", minutes = 5, target = "t".repeat(MAX_ID_LENGTH + 1))))
        rejects(SyncMessage(type = "sync", snapshot = WireSnapshot(visibility = Visibility.FULL_DETAILS, expenses = List(MAX_EXPENSES_PER_MESSAGE + 1) { Expense(id = "i$it") })))
        rejects(SyncMessage(type = "sync", snapshot = WireSnapshot(checkIns = List(MAX_CHECKINS_PER_MESSAGE + 1) { CheckIn("2026-01-01", 1) })))
        rejects(SyncMessage(type = "sync", snapshot = WireSnapshot(visibility = Visibility.FULL_DETAILS, expenses = listOf(Expense(id = "a", note = "n".repeat(MAX_TEXT_LENGTH + 1))))))
        rejects(SyncMessage(type = "sync", snapshot = WireSnapshot(name = "n".repeat(MAX_NAME_LENGTH + 1))))
        rejects(SyncMessage(type = "sync", snapshot = WireSnapshot(revision = -1)))
    }

    @Test fun negativePaiseAndNudgeMinutesRejected() {
        rejects(SyncMessage(type = "sync", snapshot = WireSnapshot(visibility = Visibility.FULL_DETAILS, expenses = listOf(Expense(id = "a", amountPaise = -1)))))
        rejects(SyncMessage(type = "sync", snapshot = WireSnapshot(dailyLimitPaise = -5)))
        rejects(SyncMessage(type = "sync", snapshot = WireSnapshot(weeklyLimitPaise = -5)))
        rejects(sync(PeerEvent(id = "s", type = "budget_suggestion", dailyPaise = -1, target = A)))
        rejects(sync(PeerEvent(id = "s", type = "budget_suggestion", weeklyPaise = MAX_PAISE + 1, target = A)))
        validateMessage(sync(PeerEvent(id = "s", type = "budget_suggestion", dailyPaise = 100, weeklyPaise = 700, target = A)))
        for (bad in listOf(0, -30, MAX_NUDGE_MINUTES + 1, Int.MAX_VALUE)) rejects(sync(PeerEvent(id = "n", type = "nudge", minutes = bad, target = A)))
        validateMessage(sync(PeerEvent(id = "n", type = "nudge", minutes = MAX_NUDGE_MINUTES, target = A)))
    }

    // ---- admin role ----
    private fun adminMsg(from: String, adminId: String, epoch: Long) = SyncMessage(type = "sync", groupId = "g1", sender = "S", adminId = adminId, adminEpoch = epoch)

    @Test fun adminTransferPropagatesWithEpochAndRejectsForgeries() {
        val a = circle(A)
        assertTrue(isAdmin(a)); assertFalse(isAdmin(circle(B)))
        assertThrows(IllegalArgumentException::class.java) { transferAdmin(circle(B), C) }
        assertThrows(IllegalArgumentException::class.java) { transferAdmin(a, A) }
        assertThrows(IllegalArgumentException::class.java) { transferAdmin(a, "zzz") }
        val t = transferAdmin(a, B)
        assertEquals(B, t.group?.adminId); assertEquals(1L, t.group?.adminEpoch); assertFalse(isAdmin(t))
        // the old admin's next sync tells C
        val c = circle(C)
        val msg = fanOut(t).first { it.member.endpointId == C }.message
        assertEquals(B, applyIncoming(c, A, msg, 5).first.group?.adminId)
        // a non-admin member cannot claim the role, even with a high epoch
        assertEquals(A, applyIncoming(c, B, adminMsg(B, B, 9), 5).first.group?.adminId)
        // stale or equal epochs lose
        val cb = applyIncoming(c, A, msg, 5).first
        assertEquals(B, applyIncoming(cb, A, adminMsg(A, A, 1), 6).first.group?.adminId)
        assertEquals(B, applyIncoming(cb, A, adminMsg(A, A, 0), 6).first.group?.adminId)
        // claims for non-members are ignored
        assertEquals(A, applyIncoming(c, A, adminMsg(A, "zzz", 3), 5).first.group?.adminId)
        // the current admin can move it again with a higher epoch
        assertEquals(C, applyIncoming(cb, B, adminMsg(B, C, 2), 7).first.group?.adminId)
    }

    @Test fun adminFallbackAndLeaveSuccessor() {
        val base = circle(A).copy(members = listOf(Member(A, "Asha", joinedAt = 1), Member(B, "B", joinedAt = 5), Member(C, "C", joinedAt = 3), Member("abc", "Abe", joinedAt = 3)))
        // auto-fallback: oldest by joinedAt, tie-break by endpoint id ("abc" < "ccc")
        val left = leaveGroup(base, now = 50)
        assertTrue(left.notices.all { it.message.adminId == "abc" && it.message.adminEpoch == 1L })
        val explicit = leaveGroup(base, successorId = B, now = 50)
        assertTrue(explicit.notices.all { it.message.adminId == B })
        assertTrue("invalid successor falls back", leaveGroup(base, successorId = "zzz", now = 50).notices.all { it.message.adminId == "abc" })
        // a member applies the leave: admin changes in the same step, A tombstoned
        val rcv = applyIncoming(circle(C, ids = listOf(A, B, C, "abc")), A, explicit.notices.first { it.member.endpointId == C }.message, 60).first
        assertEquals(B, rcv.group?.adminId); assertFalse(isActiveMember(rcv, A)); assertEquals(1L, rcv.group?.adminEpoch)
        // a non-admin leaving does not touch the admin
        assertTrue(leaveGroup(circle(B), now = 5).notices.all { it.message.adminId.isEmpty() && it.message.adminEpoch == 0L })
        // recorded admin gone and no claim yet: everyone derives the same fallback
        val gone = circle(B).copy(members = circle(B).members.map { if (it.endpointId == A) it.copy(status = MEMBER_LEFT) else it })
        assertEquals(B, currentAdminId(gone)); assertTrue(isAdmin(gone))
        assertEquals(C, currentAdminId(circle(C).copy(members = listOf(Member(B, "B", status = MEMBER_LEFT), Member(C, "C", joinedAt = 2), Member(A, "A", status = MEMBER_LEFT)))))
        // successor claims once the admin has left, but not while the admin is active
        val cs = circle(C)
        assertEquals(A, applyIncoming(cs, B, adminMsg(B, B, 1), 5).first.group?.adminId)
        val aGone = cs.copy(members = cs.members.map { if (it.endpointId == A) it.copy(status = MEMBER_LEFT) else it })
        assertEquals(B, applyIncoming(aGone, B, adminMsg(B, B, 1), 5).first.group?.adminId)
    }

    @Test fun membersCannotRemoveEachOther() {
        val a = circle(A)
        val forged = SyncMessage(type = "leave", groupId = "g1", members = listOf(Member(C, "Cee", status = MEMBER_LEFT, leftAt = 9)))
        assertTrue(isActiveMember(applyIncoming(a, B, forged, 5).first, C))
        val own = SyncMessage(type = "leave", groupId = "g1", members = listOf(Member(B, "Bee", status = MEMBER_LEFT, leftAt = 9)))
        assertFalse(isActiveMember(applyIncoming(a, B, own, 5).first, B))
    }

    // ---- visibility ----
    @Test fun visibilitySnapshotContents() {
        val now = System.currentTimeMillis()
        val today = istDate(now)
        val st = circle(A).copy(dailyLimitPaise = 500, weeklyLimitPaise = 3000, revision = 3,
            expenses = listOf(Expense(id = "e1", date = today, amountPaise = 300, merchant = "SecretCafe", reason = "Impulse", category = "Food", note = "private"),
                Expense(id = "e2", date = today, amountPaise = 100, merchant = "Bus", kind = "refund")),
            checkIns = listOf(CheckIn(today, now)))
        val totals = snapshotFor(st, Visibility.TOTALS_STATUS, now)
        assertTrue(totals.expenses.isEmpty()); assertEquals(Visibility.TOTALS_STATUS, totals.visibility)
        assertEquals(200L, totals.dailySpentPaise); assertTrue(totals.weeklySpentPaise >= 200L)
        assertEquals(500L, totals.dailyLimitPaise); assertEquals(1, totals.checkIns.size); assertEquals(1, totals.streak)
        val json = String(encodeMessage(SyncMessage(type = "sync", groupId = "g1", snapshot = totals)))
        listOf("SecretCafe", "Impulse", "private", "Food", "Bus").forEach { assertFalse("leaked $it", json.contains(it)) }
        val full = snapshotFor(st, Visibility.FULL_DETAILS, now)
        assertEquals(2, full.expenses.size); assertEquals("SecretCafe", full.expenses[0].merchant); assertEquals(200L, full.dailySpentPaise)
        assertEquals("default is totals", Visibility.TOTALS_STATUS, snapshotOf(st, now).visibility)
        assertEquals(Visibility.FULL_DETAILS, snapshotOf(st.copy(visibility = Visibility.FULL_DETAILS), now).visibility)
        validateMessage(SyncMessage(type = "sync", snapshot = totals)); validateMessage(SyncMessage(type = "sync", snapshot = full))
        // receiver keeps the totals and sees no records
        val got = applyIncoming(circle(B), A, sync(from = A).copy(snapshot = totals), 5).first.friends[A]!!
        assertTrue(got.expenses.isEmpty()); assertEquals(200L, got.dailySpentPaise); assertEquals(1, got.streak)
    }

    @Test fun switchingVisibilityBumpsRevisionOnlyOnChange() {
        val s = circle(A)
        val full = setVisibility(s, Visibility.FULL_DETAILS)
        assertEquals(s.revision + 1, full.revision)
        assertSame(full, setVisibility(full, Visibility.FULL_DETAILS))
        val back = setVisibility(full, Visibility.TOTALS_STATUS)
        // the next sync replaces the detailed copy peers hold
        val peer = applyIncoming(applyIncoming(circle(B), A, sync(from = A).copy(snapshot = snapshotFor(full, Visibility.FULL_DETAILS)), 5).first, A,
            sync(from = A).copy(snapshot = snapshotOf(back)), 6).first
        assertTrue(peer.friends[A]!!.expenses.isEmpty())
    }

    // ---- gossip-ready signed broadcast ----
    private class FakeSigner(private val self: String) : MessageSigner {
        private fun mac(id: String, p: ByteArray) = java.security.MessageDigest.getInstance("SHA-256").digest("$id|".toByteArray() + p).joinToString("") { "%02x".format(it) }
        override fun sign(payload: ByteArray) = mac(self, payload)
        override fun verify(endpointId: String, payload: ByteArray, signature: String) = signature == mac(endpointId, payload)
    }

    @Test fun gossipTopicIsDeterministicAndSecretDependent() {
        val g = Group("g1", "Crew", "sec")
        assertEquals(32, gossipTopicId(g).size)
        assertArrayEquals(gossipTopicId(g), gossipTopicId(g.copy(name = "Renamed", adminId = "x", adminEpoch = 4)))
        assertFalse(gossipTopicId(g).contentEquals(gossipTopicId(g.copy(secret = "sec2"))))
        assertFalse(gossipTopicId(g).contentEquals(gossipTopicId(g.copy(id = "g2"))))
    }

    @Test fun signedBroadcastAcceptedAndForgeriesRejected() {
        val (b2, msg) = buildBroadcast(circle(B), FakeSigner(B), 5)
        msg!!
        assertEquals(1L, b2.outSeq); assertEquals(B, msg.senderId); assertEquals(1L, msg.seq)
        val rcv = FakeSigner(A) // verification does not depend on the verifier's own identity
        val a = circle(A)
        val (a1, _) = applyBroadcast(a, decodeMessage(encodeMessage(msg)), rcv, 6)
        assertEquals(1L, a1.lastSeqSeen[B]); assertNotNull(a1.friends[B])
        fun fails(m: SyncMessage, st: BuddyState = a) = assertThrows(IllegalArgumentException::class.java) { applyBroadcast(st, m, rcv, 6) }
        fails(msg.copy(sig = ""))                                           // unsigned
        fails(msg.copy(snapshot = msg.snapshot!!.copy(dailyLimitPaise = 1))) // tampered body
        fails(msg.copy(sender = "Mallory"))                                 // tampered label
        fails(msg.copy(senderId = C))                                       // claims another member
        fails(msg.copy(seq = 2))                                            // seq is covered by the signature
        fails(signMessage(msg.copy(sig = ""), B, 1, FakeSigner(C)))         // signed with the wrong key
        fails(signMessage(msg.copy(sig = ""), "stranger", 1, FakeSigner("stranger"))) // valid signature, not a member
        fails(msg.copy(type = "join_request", joinNonce = "x", members = listOf(Member(B, "b")))) // not a broadcast type
        fails(msg, a1)                                                      // replay
        fails(signMessage(msg.copy(sig = ""), B, 0, FakeSigner(B)))         // seq must be positive
        // newer sequence number is accepted, older one after it is not
        val (b3, m2) = buildBroadcast(b2, FakeSigner(B), 7)
        val a2 = applyBroadcast(a1, m2!!, rcv, 8).first
        assertEquals(2L, a2.lastSeqSeen[B]); assertEquals(2L, b3.outSeq)
        fails(msg, a2)
        assertNull(buildBroadcast(BuddyState(), FakeSigner(B)).second)
    }

    @Test fun signedLeaveAndSignedMembershipTravelOverBroadcast() {
        val signer = FakeSigner(A)
        val left = leaveGroup(circle(A), signer = signer, now = 9)
        val bc = left.broadcast!!
        assertEquals(A, bc.senderId); assertTrue(bc.seq > 0); assertEquals(bc.seq, left.state.outSeq)
        val after = applyBroadcast(circle(B), bc, FakeSigner(B), 10).first
        assertFalse(isActiveMember(after, A))
        assertEquals(B, currentAdminId(after))
    }
}
