if [ ! -d "bin" ]; then
	mkdir bin
fi

javac -cp lib/gson-2.9.1.jar:lib/goldilocks8.jar src/model/*.java -d bin/
javac -cp lib/gson-2.9.1.jar:lib/goldilocks8.jar:bin/ src/service/*.java -d bin/
javac -cp lib/gson-2.9.1.jar:lib/goldilocks8.jar:bin/ src/*.java -d bin/


cd bin

jar -cvmf ../MANIFEST.MF gop.jar *.class model/*.class service/*.class
mv gop.jar ../.
