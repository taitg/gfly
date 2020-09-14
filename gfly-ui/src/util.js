export function headingToString(heading) {
  if (heading === undefined) return '-';
  if (heading >= 11.25 && heading < 33.75)
    return "NNE";
  if (heading >= 33.75 && heading < 56.25)
    return "NE";
  if (heading >= 56.25 && heading < 78.75)
    return "ENE";
  if (heading >= 78.75 && heading < 101.25)
    return "E";
  if (heading >= 101.25 && heading < 123.75)
    return "ESE";
  if (heading >= 123.75 && heading < 146.25)
    return "SE";
  if (heading >= 146.25 && heading < 168.75)
    return "SSE";
  if (heading >= 168.75 && heading < 191.25)
    return "S";
  if (heading >= 191.25 && heading < 213.75)
    return "SSW";
  if (heading >= 213.75 && heading < 236.25)
    return "SW";
  if (heading >= 236.25 && heading < 258.75)
    return "WSW";
  if (heading >= 258.75 && heading < 281.25)
    return "W";
  if (heading >= 281.25 && heading < 303.75)
    return "WNW";
  if (heading >= 303.75 && heading < 326.25)
    return "NW";
  if (heading >= 326.25 && heading < 348.75)
    return "NNW";
  return "N";
}