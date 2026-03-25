package com.example.foodexpiryapp.domain.model

enum class EventType {
    ITEM_ADDED,
    ITEM_EATEN,
    ITEM_DELETED,
    ITEM_EXPIRED,
    NOTIFICATION_SENT,
    NOTIFICATION_OPENED,
    SCAN_SUCCESS,
    SCAN_FAILURE,
    SEARCH_QUERY,
    APP_OPENED,
    SCREEN_VIEW,
    RECIPE_VIEWED,
    RECIPE_COOKED
}
