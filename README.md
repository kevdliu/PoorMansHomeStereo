# Poor Man's Home Stereo
Poor Man’s Home Stereo enables users who cannot afford (or are too lazy to buy) a full home stereo system to still enjoy some of the experience by allowing Android phones to serve as speakers and controllers. By arranging the phones around the house, a user can have their music play in any room they wish by streaming music wirelessly from their controller to any of the speakers. Wired speakers can also be connected to the phones to play your music wirelessly. 

---------------------------------------

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


