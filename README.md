# Paint

This is a barebones, minimalistic clone of Microsoft Paint. Only the essentials, with a special focus on creating static images (PNG/JPEG/etc.) and animations (GIF only for now).

Microsoft Paint is a bit frustrating for me. For example, why can't I draw in invisible ink for PNG's? Honestly, that question alone prompted me to make this tool.

Here is my priority list when it comes to building features.



* SHOWSTOPPER -- PNG, GIF, Pen, Eraser, Zoom Options (Minimal), Pen Size, Color Chooser (WITH INVISIBLE INK FOR APPLICABLE FORMATS), Save (image), Open (image), Cancel saving, Scroll Guards

* MUST-HAVE -- Animations (GIF only for now), JPEG, Request Dimensions, Bucket, Eyedropper, Text, Rotation, Crop, Flip, Copy/Cut/Paste (whole image), Image and pixel metrics upon hover

* SHOULD-HAVE -- Earlier warnings, Resize Image, Select area, Copy/Cut/Paste (selection), Line/Rectangle/Oval Shape tool, Different Brush types, all remaining Java supported image formats

* WANT-TO-HAVE -- Ruler, Grid, More Shapes, More Brush Types, more image formats

* NICE-TO-HAVE -- Thumbnail, Shape Builder, Brush Builder

---

By the way, some notes.

Keyboard controls are active, with expected functionality (space to draw, backspace to erase, etc). NUMPAD is supported too.

Each computer will vary, but I have this application built to handle images of at least 10k by 10k pixels. My program can technically handle more than that, but that is machine dependent.
