sed -i 's/query = queryText,/placeName = mapsResult.placeName,\n            address = mapsResult.address,/' /app/applet/app/src/test/java/com/example/TraceTest.kt
sed -i 's/urlInfo.query/urlInfo.placeName/' /app/applet/app/src/test/java/com/example/TraceTest.kt

sed -i 's/query = queryText,/placeName = mapsResult.placeName,\n            address = mapsResult.address,/' /app/applet/app/src/test/java/com/example/PorjaiTraceTest.kt
sed -i 's/urlInfo.query/urlInfo.placeName/' /app/applet/app/src/test/java/com/example/PorjaiTraceTest.kt
