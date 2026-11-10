# Only for the dead-code report (./gradlew deadCode): R8 removes everything
# the app can't reach. No optimizing (inlining would make used code look
# unused) and no renaming, so the report shows real names.
-dontoptimize
-dontobfuscate
