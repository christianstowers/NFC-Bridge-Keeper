NFC Bridge Keeper

This project is being used to explore using Host Card Emulation to create a peer-2-peer interaction between two instances of the same application which exchanges a user-provided string of data. 
The original goals were to create an app that would provide two communication and would validate the received data against a local collection and set of validation rules, but as of 10/28/2024 I 
wasnt't yet able to get that working. More to come on that. 

Currently, the delineation between the HCE card actor and NFC reader versions of the app exist on separate branches. 
- feature/initial-build contains the MainActivity version for HCE and is titled as HCE Card Tool.
- feature/reader-mode contains the MainActivity version for NFC reader functionality and is titled as NFC Reader Tool.

![Screenshot 2024-10-28 at 11 55 09 AM](https://github.com/user-attachments/assets/6c8c208d-c8a6-436a-91ba-123d371e6201)
![Screenshot 2024-10-28 at 11 55 16 AM](https://github.com/user-attachments/assets/d1f8ee06-ef21-4c7f-9efc-98b4ae7fe9ee)


Usage steps:
1. two physical Android devices with NFC hardware are required.
2. check out the feature/initial-build branch and run the app on one of the two devices.
3. check out the feature/reader-mode branch and run the app on the other of the two devices.
4. type any string of characters in the text field of the HCE Card Tool app.
5. bring the two devices back to back to initiate the NFC communication and transaction. You may need to move the devices around to find the "sweet spot" where the NFC fields can properly intersect.
6. if successful, the received text should now be displayed on the screen of the Reader Tool and will be apparent in the logs. Notice as well that the APDU communication, input string state, and NFC transaction are all present within the logs.

Upcoming feature work:
1. Having one version of the app on main that enables the user to select via UI indication whether an app instance is in HCE card mode or in Reader mode.
2. Two-way communication to meet the original goal. 
3. Publishing a Flutter version to allow for cross-platform communication. This theoretically should be possibly as the messaging format is currently NDEF.
