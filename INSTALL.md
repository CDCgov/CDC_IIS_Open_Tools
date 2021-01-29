# Installing the Source Code
This code relies on validation-report1.0.0.jar found in the NIST [v2-validation github](https://github.com/usnistgov/v2-validation). 
This jar file is not yet available through a publicly accessible maven repository.
To install this file in your local maven cache, run the following command:
```
  mvn install:install-file -Dfile=lib\validation-report-1.0.0.jar -DgroupId=com.github.hl7-tools -DartifactId=validation-report -Dversion=1.0.0 -Dpackaging=jar
```
NOTE: You can also just run install-lib.cmd from a command prompt on Windows.

# Using this project with Eclipse

1. Clone this repository to your local machine.
```
   git clone https://github.com/CDCGov/CDC_IIS_Open_Tools.git
```
2. Start Eclipse
3. Right click on the Project Explorer tab and select Import
4. Select the CDC_IIS_Open_Tools folder where the project lives

The project should be imported into Eclipse at this stage.

# Java Version
This project was build using OpenJDK 11.0.3. 
