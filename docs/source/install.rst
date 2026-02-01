Install
=======

Packages (DEB/RPM)
------------------

Download the package from Releases and install:

::

   # DEB
   sudo dpkg -i gop_|release|_amd64.deb
   sudo apt -f install

   # RPM
   sudo rpm -ivh gop-|release|-1.x86_64.rpm

Check version:

::

   gop version

Notes
-----

- jpackage builds include a runtime, so you do not need a JDK to run.
- Installer builds must be produced on their target OS.
- Sample configs are installed under:

  - Linux: ``/opt/gop/config``
  - macOS: ``/Applications/gop.app/Contents/app/config``
  - Windows: ``C:\Program Files\gop\config``

APT/YUM repo (GitHub Pages)
---------------------------

APT (Debian/Ubuntu):

::

   echo "deb [trusted=yes] https://exgoya.github.io/gop/repo/deb ./" | sudo tee /etc/apt/sources.list.d/gop.list
   sudo apt update
   sudo apt install gop

YUM/DNF (RHEL/Fedora):

::

   cat <<'EOF' | sudo tee /etc/yum.repos.d/gop.repo
   [gop]
   name=gop
   baseurl=https://exgoya.github.io/gop/repo/rpm/
   enabled=1
   gpgcheck=0
   EOF

   sudo yum install gop
