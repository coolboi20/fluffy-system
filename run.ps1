$cp = Get-Content -Path .cp.txt
$env:JAVA_HOME = "C:\Users\mpho.mahase\.jdks\openjdk-26"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
java -cp "target\classes;$cp" "-Dspring.classformat.ignore=true" com.example.fluffy.FluffyApplication --server.port=8501
