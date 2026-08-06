sed -i 's/query = queryText,/placeName = mapsResult.placeName,\n                            address = mapsResult.address,/' /app/applet/app/src/main/java/com/example/data/PlacesApiService.kt
