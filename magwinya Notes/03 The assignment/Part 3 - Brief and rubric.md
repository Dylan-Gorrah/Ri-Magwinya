# Part 3: Brief and rubric

Back to [[Part 3 - Final PoE]]

The Part 3 brief and rubric, word for word from the screenshots Dylan sent on 5 Nov 2026. The plain-English version and the to-do list are in [[Part 3 - Final PoE]].

---

**Image 1**
**Part 3 — Final Asset Development, Additions, Tweaking and Final Submission (Marks: 100)**

*Learning Units 1 to 4*

*At the end of this specific part, students should be able to:*
*   *Use blob storage.*
*   *Use a NoSQL database to store data.*
*   *Use a NoSQL database to read data.*
*   *Prepare an app for publication in the Play Store.*

The final app needs to run on a mobile phone and not an emulator. This means that you need to plan well in advance to record the required video on a mobile phone if you don't own an Android phone yourself.

For this submission, the app should be a fully working app without errors that has been updated based on the feedback your lecturer has provided you throughout the course. The app must **compile** and **run** for marks to be awarded for the app's features.

**Image 2**
For this submission, the app should be a fully working app without errors that has been updated based on the feedback your lecturer has provided you throughout the course. The app must **compile** and **run** for marks to be awarded for the app's features.

You must create a demonstration video showing that the application is working. For the video, make sure that you show the app performing the following functions:

*   The user must be able to **register** and **log in** to the app. Make sure to encrypt the password
*   Make use of single sign-on (**SSO**) **(PoE only)**.
*   The user must be able to change their **settings** in the app.
*   Connect to a **REST API** you create that is connected to a database, or use any that fits your idea that exists on the internet.
*   **Offline mode** with Sync: Create a feature that allows users to perform offline actions with synchronisation capabilities once they reconnect. This does not need to be the whole app, but at least some features. Can make use of RoomDB or SQLite **(PoE only)**.
*   Implementation of **Real-time Notification**: Implement a push notification system for real-time updates and alerts **(PoE only)**.
*   **Multi-language** support that includes at least two South African languages **(PoE only)**.
*   Your features that were specified in Part 1. Look at the rubric to see the weighting.
*   An app icon and final image assets.

**Image 3**
You may consult any resources of your choice to implement push **notifications**. These resources may assist you:

*   Push Notifications: https://firebase.google.com/docs/cloud-messaging [Accessed on 03 November 2025].

Include in the demonstration video that will effectively demonstrate all the features of the app and be fully professional. The video must include a voice-over explaining what you are showing. The video must also show what data is stored in the online-hosted authentication service, API, and database.

The app must have a user-friendly and appropriate user interface that can handle invalid inputs made by the user without crashing. The app must be working.

Version Control with GitHub: Initialise the repository with a **README file**. **Commit** and **push** your project files to the **GitHub repository**. Regularly commit and push your code as you make progress. Since this is the final PoE, make sure to tag your final commit as the ***final PoE***.

**Image 4**
Conduct automated testing on the main functionality of your app. Make use of **GitHub Actions** to run tests and build your code to make sure it will work not on just your computer. Use this guide, which will assist in creating this:

*   https://github.com/marketplace/actions/automated-build-android-app-with-github-action [Accessed on 03 November 2025].
*   https://github.com/iMAD5112/Github-actions/blob/main/.github/workflows/build.yml [Accessed on 03 November 2025].

**Documentation:**
Prepare a comprehensive report detailing the purpose of the app, its design considerations, and the utilisation of GitHub and GitHub Actions. Include all of this inside your **README file on GitHub**. Add images and make the README file your own. Release notes need to be added as well, which list all the updates that have been made to the app since the prototype. The release notes must clearly explain which of your innovative features were added to this version.

**Publishing:**

**Image 5**
The app will also need to be ready to be published on the Google Play Store.

**Submission:**
For your submission, present the following:

*   The complete **Kotlin source code** for the Android app must be submitted on GitHub. ***No zip files are allowed.***
*   Add comments to your code, and don't forget to reference them.
*   Make use of logging to show your understanding of your code.
*   Your **README file** for your app that is also on GitHub.
*   A **video presentation** showcasing all the app's features. Include a link to this video in the README file.
*   **Release notes** that list all the updates that have been made to the app since the prototype. The release notes must clearly explain which of your innovative features were added to this version.
*   **Evidence of preparation for publication:** signed APK, screenshots, and a screenshot of the app uploaded on the Google Play Console and/or published on the Google Play Store, if applicable.
*   If applicable, a **short write-up** (maximum 500 words) on the manner in which **AI tools were used and cited** during the completion of this assessment. Examples could include code snippets, debugging errors, image generation, etc.

**Image 6**
*You can upload your video to YouTube, make it unlisted, and add that link to your README file. You can also use any video recording software you like. Just make sure to include a voice-over to show your understanding.*

**Image 7**
**PORTFOLIO OF EVIDENCE (POE)**

| Marking Criteria | Does not meet the required standard | Meets the required standard | Partially exceeds the required standard | Greatly exceeds the required standard |
| :--- | :--- | :--- | :--- | :--- |
| **The app runs on a mobile device.**<br><br>[5 Marks] | • The app doesn't run at all.<br>• The app runs, but lots of crashes happen.<br><br>**0-1 Marks** | • The app runs with one or two errors or crashes.<br>• Runs on the emulator.<br><br>**2-3 Marks** | • The app runs with very infrequent errors or crashes.<br><br>**4 Marks** | • The app runs without errors.<br><br>**5 Marks** |
| **Feature: SSO login**<br><br>[10 Marks] | • Feature not included.<br>• Feature implemented but very buggy.<br><br>**0-4 Marks** | • Feature is working mostly with some bugs.<br><br>**5 Marks** | • Feature is working with only minor bugs.<br><br>**6-7 Marks** | • Feature excellently implemented.<br><br>**8-10 Marks** |
| **Feature: Offline mode**<br><br>[10 Marks] | • Feature not included.<br>• Feature implemented but very buggy.<br><br>**0-4 Marks** | • Feature is working mostly with some bugs.<br><br>**5 Marks** | • Feature is working with only minor bugs.<br><br>**6-7 Marks** | • Feature excellently implemented.<br><br>**8-10 Marks** |
| **Feature: Real-time Notification**<br><br>[10 Marks] | • Feature not included.<br>• Feature implemented but very buggy.<br><br>**0-4 Marks** | • Feature is working mostly with some bugs.<br><br>**5 Marks** | • Feature is working with only minor bugs.<br><br>**6-7 Marks** | • Feature excellently implemented.<br><br>**8-10 Marks** |

**Image 8**
**PORTFOLIO OF EVIDENCE (POE)**

| Marking Criteria | Does not meet the required standard | Meets the required standard | Partially exceeds the required standard | Greatly exceeds the required standard |
| :--- | :--- | :--- | :--- | :--- |
| **Feature: Real-time Notification**<br><br>[10 Marks] | • Feature not included.<br>• Feature implemented but very buggy.<br><br>**0-4 Marks** | • Feature is working mostly with some bugs.<br><br>**5 Marks** | • Feature is working with only minor bugs.<br><br>**6-7 Marks** | • Feature excellently implemented.<br><br>**8-10 Marks** |
| **Feature: Multi-language**<br><br>[10 Marks] | • Feature not included.<br>• Feature implemented but very buggy.<br><br>**0-4 Marks** | • Feature is working mostly with some bugs.<br><br>**5 Marks** | • Feature is working with only minor bugs.<br><br>**6-7 Marks** | • Feature excellently implemented.<br><br>**8-10 Marks** |
| **Feature: User Defined 4**<br><br>[10 Marks] | • Feature not included.<br>• Feature implemented but very buggy.<br><br>**0-4 Marks** | • Feature is working mostly with some bugs.<br><br>**5 Marks** | • Feature is working with only minor bugs.<br><br>**6-7 Marks** | • Feature excellently implemented.<br><br>**8-10 Marks** |
| **Feature: User Defined 5**<br><br>[10 Marks] | • Feature not included.<br>• Feature implemented but very buggy.<br><br>**0-4 Marks** | • Feature is working mostly with some bugs.<br><br>**5 Marks** | • Feature is working with only minor bugs.<br><br>**6-7 Marks** | • Feature excellently implemented.<br><br>**8-10 Marks** |
| **The app has been prepared for publication in the Play Store.**<br><br>[5 Marks] | • No preparation has been done.<br>• No evidence of preparation has been submitted.<br>• The app is not ready for publication.<br><br>**0-1 Marks** | • The minimum requirements for Play Store publication are met.<br><br>**2-3 Marks** | • The minimum requirements for Play Store publication are met.<br>• Some additional material is included.<br><br>**4 Marks** | • The app is completely ready to be published to the Play Store.<br>• All material, including screenshots for the Play Store, is included.<br><br>**5 Marks** |

**Image 9**
**PORTFOLIO OF EVIDENCE (POE)**

| Marking Criteria | Does not meet the required standard | Meets the required standard | Partially exceeds the required standard | Greatly exceeds the required standard |
| :--- | :--- | :--- | :--- | :--- |
| **AI-use writeup**<br><br>[5 Marks] | • Writeup not included.<br><br>**0-1 Marks** | • Writeup included, but the details provided are vague.<br><br>**2-3 Marks** | • Writeup does not include details of how AI was used and cited.<br><br>**4 Marks** | • A detailed write-up which includes information on how it was used and cited.<br>• Mark may be awarded if AI is not used at all.<br><br>**5 Marks** |
| **User interface**<br><br>[10 Marks] | • Not user-friendly, confusing, and illogical.<br>• Somewhat user-friendly with lots of mistakes in the design.<br><br>**0-4 Marks** | • Mostly user-friendly, with some mistakes in the design.<br>• Somewhat consistent use of layout, fonts, and colours.<br><br>**5 Marks** | • Mostly user-friendly, with some attempt made at making the capture process easier for the user.<br>• Mostly consistent use of layout, fonts, and colours.<br><br>**6-7 Marks** | • Excellent and user-friendly design.<br>• Completely consistent use of layout, fonts, and colours.<br><br>**8-10 Marks** |
| **GitHub, Readme and automated testing**<br><br>[10 Marks] | • Zip file added or file upload used for GitHub.<br><br>**0-4 Marks** | • Multiple commits and unit testing are not working.<br>• No readme file.<br><br>**5 Marks** | • Automated testing includes, but is not working correctly.<br>• Somewhat of a readme file created.<br><br>**6-7 Marks** | • Readme file created and implemented correctly.<br>• Automated testing is in place.<br>• Multiple commits.<br><br>**8-10 Marks** |

**Image 10**
**PORTFOLIO OF EVIDENCE (POE)**

| Marking Criteria | Does not meet the required standard | Meets the required standard | Partially exceeds the required standard | Greatly exceeds the required standard |
| :--- | :--- | :--- | :--- | :--- |
| **Demonstration video**<br><br>[5 Marks] | • No demonstration video included.<br>• Informal and unprepared, or not showing all the features or no voice-over included.<br><br>**0-1 Marks** | • It is not completely professional, but all features are demonstrated.<br><br>**2-3 Marks** | • Mostly professional with some issues, but all features were demonstrated.<br><br>**4 Marks** | • Fully professional video showing all the required features in detail.<br><br>**5 Marks** |