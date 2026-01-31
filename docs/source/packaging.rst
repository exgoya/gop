Packaging
=========

fat-jar
-------

::

   ./gradlew shadowJar

shadow zip
----------

::

   ./gradlew shadowDistZip

native app-image (Linux/macOS)
------------------------------

::

   ./gradlew jpackageAppImage

installers
----------

::

   ./gradlew jpackageDeb
   ./gradlew jpackageRpm
   ./gradlew jpackageDmg
   ./gradlew jpackagePkg
   ./gradlew jpackageMsi

Note: installer builds must be run on their target OS.
