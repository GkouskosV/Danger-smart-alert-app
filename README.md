# Danger smart alert android app

The app is still in progress.

The android app notify other people if the user is in danger. The form of the risk is set to be:
1. If the user is falling.
2. In case there is an earthquake near the user.

The app, in order to realize that any of the above dangers are occured uses the accelerometer sensor. 
* In the first case, if the vertical move of the sensor is bigger than the horizontal then the app will know that the user is falling. 
* In the latter if both horizontal and vertical axes are changing simultaneously then the app will conclude an earthquake may occured. Then a countdown of 30 seconds will begin, until an SOS message will be sent to the given friend list that the user added. The user can stop the countdown in case of false alarm. It also features sound notification generated directly after the alarm. 

### Future features

Every time a danger will occur, the app will check if also other users' mobile sernors had the same movement so to be more accurate for the earthquake occurance. Then an autogenerated text message will be sent to some people that the user has entered. 
