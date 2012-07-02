Map creation tutorial
=====================

There are two formats for map definition - png image and text file

PNG image
---------

Maps can be encoded as 8-bit RGB images in any readable format, but PNG
format is preferred. Each pixel corresponds to a particular cell in the 
map. The color of the pixel determines the status of the cell.

 * Empty: any grayscale color with the value above 200
 * Wall: any grayscale color with the value below 200
 * HQ and Flag: Any non-grayscale color is converted to HSB colorspace.
   The Hue of the color is rounded to one of the 25 possible values 
   (maximum number of teams) and determines the team. The Brightness 
   (or Value) component of the color defines the object type. If it is
   lower than 128 the object is HQ, otherwise it is Flag.

Even though the Hue component is quantized, it is recommended that the 
colors for each team vary only in Brightness/Value component. Also, 
avoid using colors that are too similar. For two teams colors like red
and blue are recommended and for four teams red, green, blue and pink.

Text file
---------

The text file format encodes the map as a series of readable ASCII 
characters. The number of rows is the height of the map and the length
of the longest row is the width of the map. All shorter lines are 
extended with empty cell symbols. All characters that are not recognized
are ignored (threated as white space).

 * Empty: white space char
 * Wall: hash char (#)
 * HQ and Flag: Each letter of English alphabet is mapped to a specific team.
   If the letter is uppercase, the object is HQ, otherwise it is a Flag.
