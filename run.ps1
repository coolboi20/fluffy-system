if (!(Test-Path .cp.txt)) {
    Write-Host "Generating classpath..."
    .\mvnw.cmd dependency:build-classpath -Dmdep.outputFile=.cp.txt
}
$cp = Get-Content -Path .cp.txt
java -cp "target\classes;$cp" com.example.fluffy.FluffyApplication --server.port=8501
