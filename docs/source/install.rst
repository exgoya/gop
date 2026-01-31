Install
=======

Packages (DEB/RPM)
------------------

Download the package from Releases and install:

::

   # DEB
   sudo dpkg -i gop_1.0.0_amd64.deb
   sudo apt -f install

   # RPM
   sudo rpm -ivh gop-1.0.0-1.x86_64.rpm

Check version:

::

   gop version

Notes
-----

- jpackage builds include a runtime, so you do not need a JDK to run.
- Installer builds must be produced on their target OS.
