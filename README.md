# Poor Man's Home Stereo
Poor Man’s Home Stereo enables users who cannot afford (or are too lazy to buy) a full home stereo system to still enjoy some of the experience by allowing Android phones to serve as speakers and controllers. By arranging the phones around the house, a user can have their music play in any room they wish by streaming music wirelessly from their controller to any of the speakers. Wired speakers can also be connected to the phones to play your music wirelessly. 

---------------------------------------

### Testing Experience

Running the Android lint check provided us with a long list of non-critical issues that, even though are not show-stopper issues that might cause crashes, still should be fixed for greater modularity and efficiency. Issues such as changing hard-coded texts to values in strings.xml (for translation), changing relative view anchors from “left” and “right” to “start” and “end” (for right-to-left layout support), and adding content description for imageviews greatly improved the internationalization and accessibility of our application and widened our user base. We also fixed numerous issues to improve performance and fixed some “grey area” code- code that works but are not using the APIs like they were designed to. One issue with lint itself, however, is that it can be very inflexible and not all of its checks makes sense for all situations. We further documented about this in the tests/README.md file. All in all, lint proved itself to be a very useful check that all developers should run during development and certainly before publishing their applications for production. 


Integrating [Crashlytics](https://fabric.io/kits/ios/crashlytics?utm_campaign=crashlytics-marketing&utm_medium=natural) for our application was surprisingly effortless thanks to their Android Studio plugin that automates the needed code insertions. Crashlytics will be a very useful tool in tracking down and fixing bugs and crashes users encounter in production. Useful information such as device model, Android version, and the full stacktrace are all collected and sent to the web dashboard for a comprehensive analytics of the problem. This is especially useful for Android applications where there are simply too many variables (version fragmentation, device configurations etc.) for a developer to thoroughly emulate and test. Catching the bugs caused by those variables and fixing them quickly is the next best thing. 


[Robotium](https://github.com/RobotiumTech/robotium) was a bit harder to integrate into the application. The installation documentation was inaccurate at times and did not cover some of the issues encountered. Once it was working it was very easy to use. Robotium offers a wide range of utility methods for simulating user interactions, such as clicking and swiping, that proved useful for testing that the different components interacted properly. 


The last tool we used was the [Android UI/Application Exerciser 
](https://developer.android.com/studio/test/monkey.html) tool to stress test our UI. Monkey generates a stream of pseudo-random user interaction events, both within the app and outside of it. This sort of test is especially useful for catching bugs caused by a user not performing certain tasks in the expected order. Testing this manually is much harder since we already know what the expected order is, and thus will not encounter as many of the invalid sequences that Monkey can produce.  We found [one bug](https://github.com/kevdliu/PoorMansHomeStereo/blob/dev/tests/monkey_reports/Monkey_bug.png) from the runs: it is possible for two tabs to be marked as selected. We will investigate this bug to determine under what conditions it is caused, and correct it. 


### Update 10/28/2016
This week we focused mainly on fixing issues reported by the lint checker and also integrating Crashlytics into the application. As of today, the majority of production code lint errors
are fixed. A few exceptions were made such as ignoring issues inside Crashlytics files and ignoring spelling errors. We are also preparing to start writing unit/instrumentation tests
to further eliminate any bugs that may exist in the application. 

### Update 10/21/2016
This week, we focused on adding features to the speaker component of the application, such as speaker-side song information display and playback controls (play, pause, rewind, and skip)
Furthermore, we added speaker QR code generation and controller QR code scanning to make the pairing process easier when UDP broadcasts are disabled on the network. 
Next week, we will begin unit and instrumentation testing in addition to work on speaker synchronization. 


### Update 10/14/2016
This week we worked on implementing the minimum viable product. The majority of the basic backend features were implemented for the speaker. The controller frontend and backend are mostly complete.

Next week we will focus on finishing the speaker backend and frontend. We also hope to begin tackling the challenge of synchronizing playback.

---------------------------------

### Intended Audience
Our intended audience is users with multiple Android devices who want the experience of a wireless home stereo music system. 

### Controller Screenshots 
![Speakers list](img/speakers.png) 
![Song queue](img/queue.png)
![Song list](img/songs.png)
![Song search](img/search.png)

### Speaker Screenshots
![Speakers UI](img/speaker.png)
![QR Code](img/qr_gen.png)

### Platform Architecture
![Flowchart](img/arch.png)

### Core Functionality
* Stream local music from the controller phone to any of the speaker phones over WiFi
* Connect multiple speaker phones on the same network to play in sync
    *  Speakers can be added through automatic network discovery, QR code scan, or manual IP address entry
* The controller supports searching and queueing songs for playback in addition to playback controls. The speakers support basic playback controls and current song information display. 

### Stretch Goals
* Support for playlists and browse by artist/album/genre etc.
* Playback position seeking

### Mobile Features
* Speaker (or auxiliary audio port)
* WiFi
* Storage (for music files)
* Camera (for QR code scan)

### Expected Limitations
Playback synchronization across speakers will be hard due to varying device performance and network latency


### Marketing Plan

We will initially advertise the app within the Tufts community and among the Tufts Computer Science department. Advertising will include word-of-mouth and Facebook posts.


