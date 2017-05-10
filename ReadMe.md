**************************************
*  README for River Data Visualizer  *
*      A River's Kidney project      *
*      UMSL SP17-CMPSCI4500-001      *
**************************************

SETUP
=========================================
Download the latest JDK for your architecture. 
This needs to be the latest as it is packaged with desired components in use for this project. 
Computer architecture will be referenced several times throughout this document. 
If you need help finding this, you can right click on Computer inside of your Start Menu for Windows. 
Then select the Properties tab. This will bring up all of the information you need. 
For *nix systems, visit the following link to find out information about your system:
https://askubuntu.com/questions/146621/what-is-the-equivalent-of-windows-system-properties-or-device-manager#146624

Go to and download the latest version of java: 
http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html

The java path should already be set from your new installation of java but you may go to this link for help with this:
https://www.java.com/en/download/help/path.xml
	
Now we must download the latest version of Eclipse for your architecture.
Go to: https://www.eclipse.org and download the latest version of Eclipse.

Follow through both installers normally.

Go to GitHub and pull the latest trunk code for the project.
https://github.com/hsswx7/CS4500GroupProject
This will contain the native java application and its dependencies.

Add this project to the Eclipse workspace by clicking on File->Open Projects from File System/Browse/To/Root/Folder.
Select the root of the project. This will probably look something like: /Browse/To/CS4500GroupProject.

Eclipse will default to an import with Maven. This is what we want. Click Finish.

Now we run Maven clean then Maven install by right clicking on project root folder and hovering
over the Run As... tab. Then click Maven clean. Follow up with a Run As...->Maven install. 
If we have an error that says that Maven is out of date, then we want to right click on the root of the project and hover over Maven. 
Then we want to select update project and check the box that says update dependencies and snapshots. Click finish. 
We should be good to run the project. Right click on the root directory of the project and click Run As...->Java Application. 
Eclipse should automatically make the runtime configuration for the project. 
Pick Luyten - us.deathmarine.luyten and click OK. Now the project should be starting to run. 
It may take a second for the GUI to be produced. This is normal.

Congrats, you have just run the project. You may consult the user manual for additional information on using the project.


ERROR INFO
===============================================
If you have problems with execeptions such as:
-You cannot run Maven due to having a JRE, then you must make sure that you have downloaded the latest JDK for java, not a JRE!
If you have more than one Java product installed then you may choose which one you point at in Eclipse by going to 
Window->Preferences->Expand Java Tab->Expand Installed JREs
Now you will want to select the JDK that you just downloaded. 
If it warns you about compiler compliance, go ahead and change the compiler level to the newest. 
This can be done by hand as well by expanding the compilers tab inside of the same window. 
In the top right corner you will see a compiler compliance level. Set this to 1.8 or the newest you have.

-If Eclipse says that it cannot run: *Insert strange looking class here* then chances are you are trying to run the wrong class file. 
Go back to the above 'Run As...' instructions and make sure that you select "Luyten - us.deathmarine.luyten" in the window right after you have selected Run As...->Java Application. 
It should run from here. If that doesn't work, then create a custom runtime configuration. 
This can be done by clicking on the downward facing arrow next to the Run button. Then click on 'Run Configurations'. 
Right click the Java Application tab and go to New. This has created a new configuration. 
The project field should be the name of your root project folder. 
The main class should be: 'us.deathmarine.luyten.Luyten'. 
The other options you shouldn't have to touch as it takes no parameters and you should have already resolved any Java JRE/JDK issues.

-If you have any other issues that are not discussed above, then make sure that the project's build path is pointing to the latest JDK you have. 
You may do this by right clicking on the root folder in the package explorer. 
Then select the Build Path->Configure Build Path... option. 
Select the libraries tab and click on your Java Runtime Environment or JRE. 
Click the edit button on the right once it has been highlighted. 
Select workspace default since your workspace default should be JDK1.8.* by this point. 
The project should rebuild so wait a second. 
Once done, you will hopefully see no errors and follow the above steps to run the project.
