
javac --enable-preview --source ${CURRENT_JAVA_VERSION} -d classes src/main/java/Paint/Main.java src/main/java/module-info.java

jar --create --file run/jar/Paint.jar --main-class Paint.Main -C classes .

jpackage \
    --verbose \
    --type msi \
    --name Paint \
    --input run/jar \
    --install-dir davidalayachew_applications/Paint \
    --vendor "David Alayachew" \
    --win-dir-chooser \
    --module io.github.davidalayachew.Paint \
    --module-path run/jar \
    --win-console \
    --java-options "--enable-preview" \
    --dest run/executable 
    --description "Minimalistic Paint clone, focusing only on the necessities" \

