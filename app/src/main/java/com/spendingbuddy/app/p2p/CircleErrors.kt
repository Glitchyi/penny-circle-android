package com.spendingbuddy.app.p2p

private const val IDENTITY_HELP = "This phone’s circle connection could not be prepared. Reopen Penny Circle and try again."
private const val UPDATE_HELP = "The phones could not read this update. Update Penny Circle on both phones, then try again."
private const val REACH_HELP = "Couldn't reach that phone. Delivery is approximate and needs their app to be open or online; it will retry."
private const val RETRY_HELP = "Couldn't complete that circle action. Try again."

/** UI boundary: untrusted peer text and transport diagnostics never become app instructions. */
private fun circleErrorEnglish(cause: Throwable): String {
    val text = cause.message.orEmpty()
    return when {
        text in setOf(IDENTITY_HELP, UPDATE_HELP, REACH_HELP, RETRY_HELP) -> text
        text.contains("ensureSelf", true) || text.contains("endpoint id", true) -> IDENTITY_HELP
        text.startsWith("Invalid sync message:") || text.contains("unexpected reply", true) ||
            text.contains("unexpected message", true) || text.contains("couldn't be read", true) ||
            text == "Reply is for a different circle." || text == "The admin is not an active member." -> UPDATE_HELP
        cause is kotlinx.coroutines.TimeoutCancellationException || text.contains("timeout", true) ||
            text.contains("timed out", true) || text.contains("network", true) ||
            text.contains("connect", true) || text.contains("unreachable", true) -> REACH_HELP
        text in setOf(
            "Only the circle admin can make a code.", "Only the circle admin can approve join requests.",
            "Only the circle admin can decline join requests.", "Only the circle admin can transfer the role.",
            "That request is no longer pending.", "Already a member.", "Pick another member of your circle.",
            "Pick a member of your circle.", "You already nudged them today.",
            "You already nudged them today, or they are not in your circle.",
            "That suggestion is no longer available.", "No join request is pending.",
            "That circle code is not valid. Ask for a new one.",
            "This circle code has expired. Ask the admin for a new one.",
            "The request expired. Ask the admin for a new code.",
            "That circle code has expired or been replaced. Ask for a new one.",
            "That code is for a different circle.", "The admin declined this request.",
            "Only the circle admin can take join requests.", "You are already in this circle.",
            "This phone is not in a circle.", "This phone left the circle and cannot rejoin with the same identity.",
            "The join request was not valid.", "Too many requests are waiting. Try again later.",
            "You are already in a circle. Leave it first.", "Already in a circle. Leave it first.",
            "Penny Circle is still getting ready. Try again.",
            "This circle is full (${com.spendingbuddy.app.MAX_MEMBERS} members).",
            "Circle names are 1 to $MAX_GROUP_NAME characters."
        ) -> text
        else -> RETRY_HELP
    }
}

/** Resolves complete, safe app copy through Android resources at runtime; pure callers may inspect the English fallback. */
fun userFacingCircleError(cause: Throwable, context: android.content.Context? = null): String {
    if (cause is CircleActionException) return cause.message.orEmpty()
    val safe = circleErrorEnglish(cause)
    if (context == null) return safe
    return when (safe) {
        IDENTITY_HELP -> context.getString(com.spendingbuddy.app.R.string.circle_error_this_phone_s_circle_connection_could_not)
        UPDATE_HELP -> context.getString(com.spendingbuddy.app.R.string.circle_error_the_phones_could_not_read_this_update)
        REACH_HELP -> context.getString(com.spendingbuddy.app.R.string.circle_error_couldnt_reach_that_phone_delivery_is_approximate)
        RETRY_HELP -> context.getString(com.spendingbuddy.app.R.string.circle_error_couldnt_complete_that_circle_action_try_again)
        "Only the circle admin can make a code." -> context.getString(com.spendingbuddy.app.R.string.circle_error_only_the_circle_admin_can_make_a)
        "Only the circle admin can approve join requests." -> context.getString(com.spendingbuddy.app.R.string.circle_error_only_the_circle_admin_can_approve_join)
        "Only the circle admin can decline join requests." -> context.getString(com.spendingbuddy.app.R.string.circle_error_only_the_circle_admin_can_decline_join)
        "Only the circle admin can transfer the role." -> context.getString(com.spendingbuddy.app.R.string.circle_error_only_the_circle_admin_can_transfer_the)
        "That request is no longer pending." -> context.getString(com.spendingbuddy.app.R.string.circle_error_that_request_is_no_longer_pending)
        "Already a member." -> context.getString(com.spendingbuddy.app.R.string.circle_error_already_a_member)
        "Pick another member of your circle." -> context.getString(com.spendingbuddy.app.R.string.circle_error_pick_another_member_of_your_circle)
        "Pick a member of your circle." -> context.getString(com.spendingbuddy.app.R.string.circle_error_pick_a_member_of_your_circle)
        "You already nudged them today." -> context.getString(com.spendingbuddy.app.R.string.circle_error_you_already_nudged_them_today)
        "You already nudged them today, or they are not in your circle." -> context.getString(com.spendingbuddy.app.R.string.circle_error_you_already_nudged_them_today_or_they)
        "That suggestion is no longer available." -> context.getString(com.spendingbuddy.app.R.string.circle_error_that_suggestion_is_no_longer_available)
        "No join request is pending." -> context.getString(com.spendingbuddy.app.R.string.circle_error_no_join_request_is_pending)
        "That circle code is not valid. Ask for a new one." -> context.getString(com.spendingbuddy.app.R.string.circle_error_that_circle_code_is_not_valid_ask)
        "This circle code has expired. Ask the admin for a new one." -> context.getString(com.spendingbuddy.app.R.string.circle_error_this_circle_code_has_expired_ask_the)
        "The request expired. Ask the admin for a new code." -> context.getString(com.spendingbuddy.app.R.string.circle_error_the_request_expired_ask_the_admin_for)
        "That circle code has expired or been replaced. Ask for a new one." -> context.getString(com.spendingbuddy.app.R.string.circle_error_that_circle_code_has_expired_or_been)
        "That code is for a different circle." -> context.getString(com.spendingbuddy.app.R.string.circle_error_that_code_is_for_a_different_circle)
        "The admin declined this request." -> context.getString(com.spendingbuddy.app.R.string.circle_error_the_admin_declined_this_request)
        "Only the circle admin can take join requests." -> context.getString(com.spendingbuddy.app.R.string.circle_error_only_the_circle_admin_can_take_join)
        "You are already in this circle." -> context.getString(com.spendingbuddy.app.R.string.circle_error_you_are_already_in_this_circle)
        "This phone is not in a circle." -> context.getString(com.spendingbuddy.app.R.string.circle_error_this_phone_is_not_in_a_circle)
        "This phone left the circle and cannot rejoin with the same identity." -> context.getString(com.spendingbuddy.app.R.string.circle_error_this_phone_left_the_circle_and_cannot)
        "The join request was not valid." -> context.getString(com.spendingbuddy.app.R.string.circle_error_the_join_request_was_not_valid)
        "Too many requests are waiting. Try again later." -> context.getString(com.spendingbuddy.app.R.string.circle_error_too_many_requests_are_waiting_try_again)
        "You are already in a circle. Leave it first." -> context.getString(com.spendingbuddy.app.R.string.circle_error_you_are_already_in_a_circle_leave)
        "Already in a circle. Leave it first." -> context.getString(com.spendingbuddy.app.R.string.circle_error_already_in_a_circle_leave_it_first)
        "Penny Circle is still getting ready. Try again." -> context.getString(com.spendingbuddy.app.R.string.circle_error_penny_circle_is_still_getting_ready_try)
        "This circle is full (${com.spendingbuddy.app.MAX_MEMBERS} members)." -> context.getString(com.spendingbuddy.app.R.string.circle_error_this_circle_is_full_members, com.spendingbuddy.app.MAX_MEMBERS)
        "Circle names are 1 to $MAX_GROUP_NAME characters." -> context.getString(com.spendingbuddy.app.R.string.circle_error_circle_names_are_1_to_characters, MAX_GROUP_NAME)
        else -> context.getString(com.spendingbuddy.app.R.string.circle_error_couldnt_complete_that_circle_action_try_again)
    }
}

internal class CircleActionException(message: String, cause: Throwable? = null) : IllegalStateException(message, cause)
