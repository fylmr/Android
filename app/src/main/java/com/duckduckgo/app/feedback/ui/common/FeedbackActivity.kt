/*
 * Copyright (c) 2019 DuckDuckGo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.duckduckgo.app.feedback.ui.common

import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.transaction
import androidx.lifecycle.Observer
import com.duckduckgo.app.browser.R
import com.duckduckgo.app.feedback.ui.initial.InitialFeedbackFragment
import com.duckduckgo.app.feedback.ui.negative.FeedbackType
import com.duckduckgo.app.feedback.ui.negative.initial.MainReasonNegativeFeedbackFragment
import com.duckduckgo.app.feedback.ui.negative.initial.SubReasonNegativeFeedbackFragment
import com.duckduckgo.app.feedback.ui.negative.openended.ShareOpenEndedNegativeFeedbackFragment
import com.duckduckgo.app.feedback.ui.positive.initial.PositiveFeedbackLandingFragment
import com.duckduckgo.app.feedback.ui.positive.openended.ShareOpenEndedPositiveFeedbackFragment
import com.duckduckgo.app.global.DuckDuckGoActivity
import kotlinx.android.synthetic.main.include_toolbar.*
import timber.log.Timber


class FeedbackActivity : DuckDuckGoActivity(),
    InitialFeedbackFragment.InitialFeedbackListener,
    PositiveFeedbackLandingFragment.PositiveFeedbackLandingListener,
    ShareOpenEndedNegativeFeedbackFragment.OpenEndedNegativeFeedbackListener,
    MainReasonNegativeFeedbackFragment.MainReasonNegativeFeedbackListener,
    SubReasonNegativeFeedbackFragment.DisambiguationNegativeFeedbackListener {

    private val viewModel: FeedbackViewModel by bindViewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feedback)
        setupActionBar()
        configureObservers()
    }

    private fun setupActionBar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun configureObservers() {
        viewModel.command.observe(this, Observer {
            it?.let { command -> processCommand(command) }
        })
        viewModel.viewState.observe(this, Observer<ViewState> {
            it?.let { viewState -> render(viewState) }
        })
    }

    private fun processCommand(command: Command) {
        Timber.v("Processing command: $command")

        when (command) {
            is Command.Exit -> {
                animateFinish(command.feedbackSubmitted)
            }
        }
    }

    private fun render(viewState: ViewState) {
        Timber.v("ViewState is: $viewState")

        val state = viewState.fragmentViewState
        when (state) {
            is FragmentState.InitialAppEnjoymentClarifier -> showInitialFeedbackView(state.direction)
            is FragmentState.PositiveFeedbackStep1 -> showPositiveFeedbackView(state.direction)
            is FragmentState.PositiveShareFeedback -> showSharePositiveFeedbackView(state.direction)
            is FragmentState.NegativeFeedbackMainReason -> showNegativeFeedbackMainReasonView(state.direction)
            is FragmentState.NegativeFeedbackSubReason -> showNegativeFeedbackSubReasonView(state.direction, state.mainReason)
            is FragmentState.NegativeOpenEndedFeedback -> showNegativeOpenEndedFeedbackView(state.direction, state.mainReason)
        }
    }

    private fun showSharePositiveFeedbackView(direction: NavigationDirection) {
        val fragment = ShareOpenEndedPositiveFeedbackFragment.instance()
        updateFragment(fragment, direction)
    }

    private fun showNegativeFeedbackMainReasonView(direction: NavigationDirection) {
        val fragment = MainReasonNegativeFeedbackFragment.instance()
        updateFragment(fragment, direction)
    }

    private fun showNegativeFeedbackSubReasonView(direction: NavigationDirection, mainReason: FeedbackType.MainReason) {
        val fragment = SubReasonNegativeFeedbackFragment.instance(mainReason)
        updateFragment(fragment, direction)
    }

    private fun showNegativeOpenEndedFeedbackView(direction: NavigationDirection, mainReason: FeedbackType.MainReason) {
        val fragment = ShareOpenEndedNegativeFeedbackFragment.instance(mainReason)
        updateFragment(fragment, direction)
    }

    private fun showInitialFeedbackView(direction: NavigationDirection) {
        val fragment = InitialFeedbackFragment.instance()
        updateFragment(fragment, direction)
    }

    private fun showPositiveFeedbackView(direction: NavigationDirection) {
        val fragment = PositiveFeedbackLandingFragment.instance()
        updateFragment(fragment, direction)
    }

    private fun updateFragment(fragment: FeedbackFragment, direction: NavigationDirection) {
        if (supportFragmentManager.findFragmentByTag(fragment.fragmentTag) != null) return

        supportFragmentManager.transaction {
            this.applyTransition(direction)
            replace(R.id.fragmentContainer, fragment, fragment.fragmentTag)
        }
    }

    override fun onBackPressed() {
        viewModel.onBackPressed()
    }

    /**
     * Initial feedback page listeners
     */
    override fun userSelectedPositiveFeedback() {
        viewModel.userSelectedPositiveFeedback()
    }

    override fun userSelectedNegativeFeedback() {
        viewModel.userSelectedNegativeFeedback()
    }

    override fun userCancelled() {
        Timber.i("User cancelled")
        viewModel.userWantsToCancel()
    }

    /**
     * Positive feedback first page listeners
     */
    override fun userSelectedToRateApp() {
        Timber.i("User gave rating")
        animateFinish(feedbackSubmitted = true)
    }

    override fun userSelectedToGiveFeedback() {
        viewModel.userSelectedToGiveFeedback()
    }

    /**
     * Positive open ended feedback listener
     */
    override fun onProvidedNegativeOpenEndedFeedback(feedback: String) {
        viewModel.userProvidedOpenEndedFeedback(feedback)
    }

    /**
     * Negative feedback disambiguation
     */
    override fun userSelectedNegativeFeedbackTypeDisambiguationMainReason(type: FeedbackType.MainReason) {
        viewModel.userSelectedNegativeFeedbackMainReason(type)
    }


    override fun userSelectedSubReasonMissingBrowser(type: FeedbackType.MissingBrowserFeaturesSubreasons) {
        viewModel.userSelectedNegativeFeedbackMissingBrowserSubReason(type)
    }

    override fun userSelectedSubReasonWebsitesNotLoading(type: FeedbackType.WebsitesNotLoading) {
        viewModel.userSelectedSubReasonWebsitesNotLoading(type)
    }

    override fun userSelectedSubReasonSearchNotGoodEnough(type: FeedbackType.SearchResultsNotGoodEnough) {
        viewModel.userSelectedSubReasonSearchNotGoodEnough(type)
    }

    override fun userSelectedSubReasonNeedMoreCustomization(type: FeedbackType.NeedMoreCustomization) {
        viewModel.userSelectedSubReasonNeedMoreCustomization(type)
    }

    override fun userSelectedSubReasonAppIsSlowOrBuggy(type: FeedbackType.AppIsSlowOrBuggy) {
        viewModel.userSelectedSubReasonAppIsSlowOrBuggy(type)
    }

    companion object {

        fun intent(context: Context): Intent {
            val intent = Intent(context, FeedbackActivity::class.java)
            return intent
        }

    }
}

private fun FeedbackActivity.animateFinish(feedbackSubmitted: Boolean) {
    val resultCode =
        if (feedbackSubmitted) {
            RESULT_OK
        } else {
            RESULT_CANCELED
        }
    setResult(resultCode)

    finish()
    overridePendingTransition(R.anim.slide_from_left, R.anim.slide_to_right)
}

private fun FragmentTransaction.applyTransition(direction: NavigationDirection) {
    if (direction.isForward) {
        setCustomAnimations(R.anim.slide_from_right, R.anim.slide_to_left)
    } else {
        setCustomAnimations(R.anim.slide_from_left, R.anim.slide_to_right)
    }
}
