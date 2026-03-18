package com.rejowan.linky.presentation.feature.batchimport

/**
 * Events for Batch Import feature
 * User actions and system events
 */
sealed class BatchImportEvent {
    // Step 1: Paste
    data class OnTextChanged(val text: String) : BatchImportEvent()
    data object OnStartScan : BatchImportEvent()

    // Step 2: Summary & Selection
    data object OnProceedToSelection : BatchImportEvent()
    data object OnBackToEdit : BatchImportEvent()

    // Selection actions
    data class OnToggleUrlSelection(val url: String) : BatchImportEvent()
    data object OnSelectAll : BatchImportEvent()
    data object OnDeselectAll : BatchImportEvent()
    data object OnSelectNewOnly : BatchImportEvent()

    // Removal actions (destructive)
    data object OnRemoveDuplicates : BatchImportEvent()
    data object OnRemoveUnselected : BatchImportEvent()
    data class OnRemoveUrl(val url: String) : BatchImportEvent()

    // Step 3: Preview Fetch
    data object OnStartFetching : BatchImportEvent()
    data object OnAutoFillTitles : BatchImportEvent()

    // Step 4: Import
    data class OnCollectionSelected(val collectionId: String?) : BatchImportEvent()
    data object OnStartImport : BatchImportEvent()
    data object OnRetryImport : BatchImportEvent()
    data object OnRetryFailed : BatchImportEvent()

    // Create collection dialog events
    data object OnCreateCollectionClick : BatchImportEvent()
    data class OnNewCollectionNameChange(val name: String) : BatchImportEvent()
    data class OnNewCollectionColorChange(val color: String?) : BatchImportEvent()
    data object OnCreateCollectionConfirm : BatchImportEvent()
    data object OnCreateCollectionDismiss : BatchImportEvent()

    // Navigation
    data object OnBack : BatchImportEvent()
    data object OnCancel : BatchImportEvent()
    data object OnDone : BatchImportEvent()
}

/**
 * UI events for Batch Import
 * One-time events that trigger UI changes
 */
sealed class BatchImportUiEvent {
    data class ShowError(val message: String) : BatchImportUiEvent()
    data class ShowConfirmation(val type: ConfirmationType) : BatchImportUiEvent()
    data object NavigateToHome : BatchImportUiEvent()
    data object NavigateToSettings : BatchImportUiEvent()
    data class NavigateToCollection(val collectionId: String) : BatchImportUiEvent()
}

/**
 * Types of confirmation dialogs
 */
enum class ConfirmationType {
    DISCARD_PASTE,
    DISCARD_SELECTION,
    CANCEL_IMPORT,
    CANCEL_AFTER_FETCH_FAILURE
}
