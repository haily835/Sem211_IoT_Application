# import Python Open Weather Map to our project.
from pyowm.owm import OWM

# source: https://stackoverflow.com/questions/24906833/how-to-access-current-location-of-any-user-using-python

import geocoder
location = geocoder.ip('me')

def dump(obj):
  for attr in dir(obj):
    print("obj.%s = %r" % (attr, getattr(obj, attr)))

# dump(location)

address = location.geojson['features'][0]['properties']['address']
lat = location.latlng[0]
lon = location.latlng[1]

owm = OWM('d841bb957d163d5d135e9f91267b8517')
mgr = owm.weather_manager()

# Get the ID of a city given its name
# reg = owm.city_id_registry()
# list_of_locations = reg.locations_for(city_name="Thanh pho Ho Chi Minh", country="VN", matching="like")
# hcm = list_of_locations[0]
one_call = mgr.one_call(lat=lat, lon=lon)

weather = {
  "temp": str(one_call.current.temperature(unit='celsius')),
  "humidity": str(one_call.current.humidity),
  "wind": str(one_call.current.wind()),
  "status": str(one_call.current.status),
  "last_check": str(one_call.current.reference_time(timeformat='iso')),
  "location": address,
}

print(weather)

