## Several issues presented by lint during the initial lint check are ignored. They are listed below with the explanation

**Type: Declaration redundancy**

Issue: Actual method parameter is the same constant

Reason: This is due to a minor application feature (not a bug) that hasn't been implemented yet. Once implemented, different values will be passed as the method parameter. 

Issue: Unused method parameters

Reason: This is due to a minor application feature (not a bug) that hasn't been implemented yet. Once implemented, the method parameters will be used

**Type: Usability**

Issue: Missing support for Firebase App Indexing

Reason: Firebase App Indexing allows Google to deep link certain web links to content within the application. 
For that, the application will first have to have some feature set to handle web links and web content, which our application does not (not designed to). Therefore, Firebase App Indexing is not useful to our application. 

**Type: Security**

Issue: AllowBackup/FullBackupContent Problems

Reason: This issue is for applications that may contain certain sensitive files that should not be backed up by the Android backup system. Our application does not have any files that should not be backed up, so no backup configuration file is needed. 

**Type: Correctness**

Issue: Layout Inflation without a Parent

Reason: Passing a parent view when inflating the layout of an AlertDialog is not possible. Due to the structure of AlertDialogs and 
AlertDialog.Builder, we have to inflate the root layout from XML ourselves before passing it to the builder. Therefore, the layout
we are inflating IS the root aka top parent view of the dialog. There is no parent for the root view we're inflating. 

Issue: Obsolete Gradle Dependency

Reason: Lint is complaining here because we are not using the latest v25 of the Android support library. However, the application target API level is recommended to be the same as the support library version. We're targeting API level 24 instead of 25 for now because 
API level 25 is still in the preview phase. 
