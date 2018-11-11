package ernestoyaquello.com.verticalstepperform;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;

import androidx.appcompat.widget.AppCompatImageButton;

import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;

import java.util.Arrays;
import java.util.List;

import ernestoyaquello.com.verticalstepperform.listener.VerticalStepperFormListener;
import ernestoyaquello.com.verticalstepperform.util.model.Step;

/**
 * Custom layout that implements a vertical stepper form.
 */
public class VerticalStepperFormLayout extends LinearLayout {

    private FormStyle style;
    private VerticalStepperFormListener listener;
    private List<ExtendedStep> steps;

    private LinearLayout content;
    private ScrollView stepsScrollView;
    private ProgressBar progressBar;
    private AppCompatImageButton previousStepButton, nextStepButton;
    private View bottomNavigation;

    public VerticalStepperFormLayout(Context context) {
        super(context);

        setupForm(context);
    }

    public VerticalStepperFormLayout(Context context, AttributeSet attrs) {
        super(context, attrs);

        setupForm(context);
    }

    public VerticalStepperFormLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        setupForm(context);
    }

    private void setupForm(Context context) {
        LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(R.layout.vertical_stepper_form_layout, this, true);

        // TODO Move all these default style values to resource files
        // TODO Get these values from XML attributes whenever possible
        FormStyle.defaultStepButtonText =
                getResources().getString(R.string.vertical_form_stepper_form_continue_button);
        FormStyle.defaultLastStepButtonText =
                getResources().getString(R.string.vertical_form_stepper_form_confirm_button);
        FormStyle.defaultConfirmationStepTitle =
                getResources().getString(R.string.vertical_form_stepper_form_confirmation_step_title);
        FormStyle.defaultAlphaOfDisabledElements = 0.25f;
        FormStyle.defaultStepNumberBackgroundColor = Color.rgb(63, 81, 181);
        FormStyle.defaultButtonBackgroundColor = Color.rgb(63, 81, 181);
        FormStyle.defaultButtonPressedBackgroundColor = Color.rgb(48, 63, 159);
        FormStyle.defaultStepNumberTextColor = Color.rgb(255, 255, 255);
        FormStyle.defaultStepTitleTextColor = Color.rgb(33, 33, 33);
        FormStyle.defaultStepSubtitleTextColor = Color.rgb(162, 162, 162);
        FormStyle.defaultButtonTextColor = Color.rgb(255, 255, 255);
        FormStyle.defaultButtonPressedTextColor = Color.rgb(255, 255, 255);
        FormStyle.defaultErrorMessageTextColor = Color.rgb(175, 18, 18);
        FormStyle.defaultDisplayBottomNavigation = true;
        FormStyle.defaultDisplayVerticalLineWhenStepsAreCollapsed = true;
        FormStyle.defaultDisplayStepButtons = true;
        FormStyle.defaultIncludeConfirmationStep = true;
    }

    /**
     * Gets an instance of the builder that will be used to set up and initialize the form.
     *
     * @param stepperFormListener The listener for the stepper form events.
     * @param steps An array with the step that will be set up in the form.
     * @return An instance of the stepper form builder.
     */
    public FormBuilder setup(VerticalStepperFormListener stepperFormListener, Step[] steps) {
        return new FormBuilder(this, stepperFormListener, steps);
    }

    void initialiseVerticalStepperForm(VerticalStepperFormListener listener, FormStyle style, ExtendedStep[] steps) {
        this.listener = listener;
        this.style = style;
        this.steps = Arrays.asList(steps);

        initialize();
    }

    public void markOpenStepAsCompleted(boolean useAnimations) {
        markStepAsCompleted(getOpenStepPosition(), useAnimations);
    }

    public void markOpenStepAsUncompleted(String errorMessage, boolean useAnimations) {
        markStepAsUncompleted(getOpenStepPosition(), errorMessage, useAnimations);
    }

    public void markStepAsCompleted(int stepPosition, boolean useAnimations) {
        ExtendedStep step = steps.get(stepPosition);
        step.markAsCompleted(useAnimations);

        updateBottomNavigationButtons();
        refreshFormProgress();
    }

    public void markStepAsUncompleted(int stepPosition, String errorMessage, boolean useAnimations) {
        ExtendedStep step = steps.get(stepPosition);
        step.markAsUncompleted(errorMessage, useAnimations);

        updateBottomNavigationButtons();
        refreshFormProgress();
    }

    public boolean isOpenStepCompleted() {
        return isStepCompleted(getOpenStepPosition());
    }

    public boolean isStepCompleted(int stepPosition) {
        return steps.get(stepPosition).isCompleted();
    }

    public boolean isAnyStepCompleted() {
        for (int i = 0; i < steps.size(); i++) {
            if (steps.get(i).isCompleted()) {
                return true;
            }
        }

        return false;
    }

    public boolean areAllPreviousStepsCompleted(int stepPosition) {
        boolean previousStepsAreCompleted = true;
        for (int i = stepPosition - 1; i >= 0; i--) {
            previousStepsAreCompleted &= steps.get(i).isCompleted();
        }

        return previousStepsAreCompleted;
    }

    public boolean goToNextStep() {
        return goToStep(getOpenStepPosition() + 1, true);
    }

    public boolean goToPreviousStep() {
        return goToStep(getOpenStepPosition() - 1, true);
    }

    public synchronized boolean goToStep(int stepPosition, boolean useAnimations) {
        if (getOpenStepPosition() != stepPosition) {
            boolean previousStepsAreCompleted = areAllPreviousStepsCompleted(stepPosition);
            if (previousStepsAreCompleted) {
                openStep(stepPosition, useAnimations);
                return true;
            }
        }

        return false;
    }

    private void openStep(int stepToOpenPosition, boolean useAnimations) {
        if (stepToOpenPosition >= 0 && stepToOpenPosition < steps.size()) {

            int stepToClosePosition = getOpenStepPosition();
            if (stepToClosePosition != -1) {
                ExtendedStep stepToClose = steps.get(stepToClosePosition);
                stepToClose.close(useAnimations);

                if (!stepToClose.isConfirmationStep()) {
                    listener.onStepClosed(stepToClosePosition, useAnimations);
                }
            }

            ExtendedStep stepToOpen = steps.get(stepToOpenPosition);
            stepToOpen.open(useAnimations);

            updateBottomNavigationButtons();
            scrollToOpenStep(useAnimations);

            if (!stepToOpen.isConfirmationStep()) {
                listener.onStepOpened(stepToOpenPosition, useAnimations);
            } else {
                refreshFormProgress();
            }
        } else if (stepToOpenPosition == steps.size()) {
            completeForm();
        }
    }

    public int getOpenStepPosition() {
        for (int i = 0; i < steps.size(); i++) {
            ExtendedStep step = steps.get(i);
            if (step.isOpen()) {
                return i;
            }
        }

        return -1;
    }

    public void removeOpenStepSubtitle(boolean useAnimations) {
        removeStepSubtitle(getOpenStepPosition(), useAnimations);
    }

    public void removeStepSubtitle(int stepPosition, boolean useAnimations) {
        updateStepSubtitle(stepPosition, "", useAnimations);
    }

    public void updateOpenStepSubtitle(String subtitle, boolean useAnimations) {
        updateStepSubtitle(getOpenStepPosition(), subtitle, useAnimations);
    }

    public void updateStepSubtitle(int stepPosition, String subtitle, boolean useAnimations) {
        if (stepPosition >= 0 && stepPosition < steps.size()) {
            ExtendedStep step = steps.get(stepPosition);
            step.updateSubtitle(subtitle, useAnimations);
        }
    }

    private void initialize() {
        progressBar.setMax(steps.size());

        if (!style.displayBottomNavigation) {
            hideBottomNavigation();
        }

        setObserverForKeyboard();

        for (int i = 0; i < steps.size(); i++) {
            final int stepPosition = i;
            ExtendedStep step = steps.get(stepPosition);
            boolean isLast = (i + 1) == steps.size();

            View.OnClickListener clickOnNextButtonListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    goToStep((stepPosition + 1), true);
                }
            };
            View.OnClickListener clickOnHeaderListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    goToStep(stepPosition, true);
                }
            };
            View stepContentLayout = !step.isConfirmationStep()
                    ? listener.getStepContentLayout(i)
                    : null;

            View stepLayout = step.initialize(
                    style,
                    getContext(),
                    content,
                    stepContentLayout,
                    clickOnNextButtonListener,
                    clickOnHeaderListener,
                    stepPosition,
                    isLast);

            content.addView(stepLayout);
        }

        goToStep(0, false);
    }

    private void setObserverForKeyboard() {
        content.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                Rect r = new Rect();
                content.getWindowVisibleDisplayFrame(r);
                int screenHeight = content.getRootView().getHeight();
                int keypadHeight = screenHeight - r.bottom;

                if (keypadHeight > screenHeight * 0.15) {
                    // The keyboard has probably been opened, so we scroll to the step
                    scrollToOpenStep(true);
                }
            }
        });
    }

    public void hideBottomNavigation() {
        bottomNavigation.setVisibility(View.GONE);
    }

    private void updateBottomNavigationButtons() {
        int stepPosition = getOpenStepPosition();
        ExtendedStep step = steps.get(stepPosition);

        if (stepPosition == 0) {
            disablePreviousButtonInBottomNavigation();
        } else {
            enablePreviousButtonInBottomNavigation();
        }

        if (step.isCompleted() && (stepPosition + 1) < steps.size()) {
            enableNextButtonInBottomNavigation();
        } else {
            disableNextButtonInBottomNavigation();
        }
    }

    public void scrollToStep(final int stepPosition, final boolean smoothScroll) {
        stepsScrollView.post(new Runnable() {
            public void run() {
                if (smoothScroll) {
                    stepsScrollView.smoothScrollTo(0, steps.get(stepPosition).getStepLayout().getTop());
                } else {
                    stepsScrollView.scrollTo(0, steps.get(stepPosition).getStepLayout().getTop());
                }
            }
        });
    }

    public void scrollToOpenStep(boolean smoothScroll) {
        scrollToStep(getOpenStepPosition(), smoothScroll);
    }

    private void refreshFormProgress() {
        int progress = 0;
        for (int i = 0; i < steps.size(); i++) {
            if (steps.get(i).isCompleted()) {
                ++progress;
            }
        }
        setProgress(progress);
    }

    private void disablePreviousButtonInBottomNavigation() {
        disableBottomButtonNavigation(previousStepButton);
    }

    private void enablePreviousButtonInBottomNavigation() {
        enableBottomButtonNavigation(previousStepButton);
    }

    private void disableNextButtonInBottomNavigation() {
        disableBottomButtonNavigation(nextStepButton);
    }

    private void enableNextButtonInBottomNavigation() {
        enableBottomButtonNavigation(nextStepButton);
    }

    private void enableBottomButtonNavigation(View button) {
        button.setAlpha(1f);
        button.setEnabled(true);
    }

    private void disableBottomButtonNavigation(View button) {
        button.setAlpha(style.alphaOfDisabledElements);
        button.setEnabled(false);
    }

    private void completeForm() {
        ExtendedStep step = steps.get(getOpenStepPosition());
        step.disableNextButton();

        listener.onCompletedForm();
    }

    public void cancelFormCompletion() {
        int openedStepPosition = getOpenStepPosition();
        if (openedStepPosition != -1 && openedStepPosition < steps.size()) {
            ExtendedStep step = steps.get(openedStepPosition);
            step.enableNextButton();
        }
    }

    public void setProgress(int progress) {
        if (progress > 0 && progress <= steps.size()) {
            progressBar.setProgress(progress);
        }
    }

    private void restoreFromState(
            int positionToOpen,
            boolean[] completedSteps,
            String[] subtitles,
            String[] errorMessages) {

        for (int i = 0; i < completedSteps.length; i++) {
            updateStepSubtitle(i, subtitles[i], false);
            if (completedSteps[i]) {
                markStepAsCompleted(i, false);
            } else {
                markStepAsUncompleted(i, errorMessages[i], false);
            }
        }

        goToStep(positionToOpen, false);
        refreshFormProgress();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        findViews();
        registerListeners();
    }

    private void findViews() {
        content = findViewById(R.id.content);
        stepsScrollView = findViewById(R.id.steps_scroll);
        progressBar = findViewById(R.id.progress_bar);
        previousStepButton = findViewById(R.id.down_previous);
        nextStepButton = findViewById(R.id.down_next);
        bottomNavigation = findViewById(R.id.bottom_navigation);
    }

    private void registerListeners() {
        previousStepButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                goToPreviousStep();
            }
        });
        nextStepButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                goToNextStep();
            }
        });
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();

        boolean[] completedSteps = new boolean[steps.size()];
        String[] subtitles = new String[steps.size()];
        String[] errorMessages = new String[steps.size()];
        for (int i = 0; i < completedSteps.length; i++) {
            ExtendedStep step = steps.get(i);

            completedSteps[i] = step.isCompleted();
            subtitles[i] = step.getSubtitle();
            if (!step.isCompleted()) {
                errorMessages[i] = step.getCurrentErrorMessage();
            }
        }

        bundle.putParcelable("superState", super.onSaveInstanceState());
        bundle.putInt("openStep", this.getOpenStepPosition());
        bundle.putBooleanArray("completedSteps", completedSteps);
        bundle.putStringArray("subtitles", subtitles);
        bundle.putStringArray("errorMessages", errorMessages);

        return bundle;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) // implicit null check
        {
            Bundle bundle = (Bundle) state;

            String[] errorMessages = bundle.getStringArray("errorMessages");
            String[] subtitles = bundle.getStringArray("subtitles");
            boolean[] completedSteps = bundle.getBooleanArray("completedSteps");
            int positionToOpen = bundle.getInt("openStep");
            state = bundle.getParcelable("superState");

            restoreFromState(positionToOpen, completedSteps, subtitles, errorMessages);
        }
        super.onRestoreInstanceState(state);
    }

    static class FormStyle {

        private static String defaultStepButtonText;
        private static String defaultLastStepButtonText;
        private static String defaultConfirmationStepTitle;
        private static float defaultAlphaOfDisabledElements;
        private static int defaultStepNumberBackgroundColor;
        private static int defaultButtonBackgroundColor;
        private static int defaultButtonPressedBackgroundColor;
        private static int defaultStepNumberTextColor;
        private static int defaultStepTitleTextColor;
        private static int defaultStepSubtitleTextColor;
        private static int defaultButtonTextColor;
        private static int defaultButtonPressedTextColor;
        private static int defaultErrorMessageTextColor;
        private static boolean defaultDisplayBottomNavigation;
        private static boolean defaultDisplayVerticalLineWhenStepsAreCollapsed;
        private static boolean defaultDisplayStepButtons;
        private static boolean defaultIncludeConfirmationStep;

        String stepButtonText;
        String lastStepButtonText;
        String confirmationStepTitle;
        float alphaOfDisabledElements;
        int stepNumberBackgroundColor;
        int buttonBackgroundColor;
        int buttonPressedBackgroundColor;
        int stepNumberTextColor;
        int stepTitleTextColor;
        int stepSubtitleTextColor;
        int buttonTextColor;
        int buttonPressedTextColor;
        int errorMessageTextColor;
        boolean displayBottomNavigation;
        boolean displayVerticalLineWhenStepsAreCollapsed;
        boolean displayStepButtons;
        boolean includeConfirmationStep;

        FormStyle() {
            this.stepButtonText = defaultStepButtonText;
            this.lastStepButtonText = defaultLastStepButtonText;
            this.confirmationStepTitle = defaultConfirmationStepTitle;
            this.alphaOfDisabledElements = defaultAlphaOfDisabledElements;
            this.stepNumberBackgroundColor = defaultStepNumberBackgroundColor;
            this.buttonBackgroundColor = defaultButtonBackgroundColor;
            this.buttonPressedBackgroundColor = defaultButtonPressedBackgroundColor;
            this.stepNumberTextColor = defaultStepNumberTextColor;
            this.stepTitleTextColor = defaultStepTitleTextColor;
            this.stepSubtitleTextColor = defaultStepSubtitleTextColor;
            this.buttonTextColor = defaultButtonTextColor;
            this.buttonPressedTextColor = defaultButtonPressedTextColor;
            this.errorMessageTextColor = defaultErrorMessageTextColor;
            this.displayBottomNavigation = defaultDisplayBottomNavigation;
            this.displayVerticalLineWhenStepsAreCollapsed = defaultDisplayVerticalLineWhenStepsAreCollapsed;
            this.displayStepButtons = defaultDisplayStepButtons;
            this.includeConfirmationStep = defaultIncludeConfirmationStep;
        }
    }
}