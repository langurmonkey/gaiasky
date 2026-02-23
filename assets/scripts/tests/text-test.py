# This script tests the displaying of custom messages and images.
# Created by Toni Sagrista

from py4j.clientserver import ClientServer, JavaParameters

gateway = ClientServer(java_parameters=JavaParameters(auto_convert=True))
gs = gateway.entry_point


"""
Prints a notice on the screen and waits for any input.
y - y coordinate of the notice in [0..1], from bottom to top
idsToRemove - List with the ids to remove when input is received.
"""
def wait_input(y, idsToRemove):
    waitid = 3634

    gs.displayMessageObject(waitid, "Press any key to continue...", 0.6, y, 0.9, 0.9, 0.0, 1.0, 25)
    gs.waitForInput()
    gs.removeObjects(idsToRemove)
    gs.removeObject(waitid)

"""
Creates a typewriter effect where text appears one letter at a time.
The parameter delay indicates the time in seconds between each letter. 
"""
def typewriter(id, text, x, y, width, height, r, g, b, a, delay):
    buffer = ""
    gs.displayTextObject(id, "", x, y, width, height, r, g, b, a, textSize)
    for letter in text:
        buffer += letter
        gs.displayTextObject(id, buffer, x, y, width, height, r, g, b, a, textSize)
        gs.sleep(delay)


# Minimize interface, disable input, stop camera
gs.enableInput()
gs.cameraStop()
gs.setVisibility("element.labels", False)

textSize = 23
gs.displayMessageObject(1, "0 - Text size 18, no scaling", 0.1, 0.9, 1.0, 0.7, 0.0, 1.0, 18)
gs.displayMessageObject(2, "1 - Text size 23, no scaling", 0.1, 0.5, 1.0, 1.0, 1.0, 1.0, 23)
gs.displayMessageObject(3, "2 - Text size 38, scaling up from 33", 0.1, 0.3, 1.0, 1.0, 1.0, 1.0, 38)
gs.displayMessageObject(4, "3 - Text size 32, scaling down from 33", 0.5, 0.5, 1.0, 1.0, 1.0, 1.0, 32)

gs.sleep(2.0)
wait_input(0.0, [1, 2, 3, 4])
text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Aenean sollicitudin felis et metus cursus, ac placerat nisi laoreet. Aenean porttitor elit velit. Curabitur cursus diam non bibendum tincidunt. Maecenas laoreet pellentesque nunc, a sollicitudin ligula lacinia vitae. Quisque eu risus est. Nulla ut risus volutpat, fermentum risus sit amet, blandit nulla. Curabitur venenatis risus sed blandit euismod. Quisque suscipit, felis quis egestas malesuada, enim orci mattis erat, a luctus felis massa eu tellus. Sed interdum pharetra gravida. Nulla lacinia malesuada neque ut cursus.";
gs.displayMessageObject(1, "Typewriter test", 0.1, 0.9, 1.0, 0.7, 0.0, 1.0, 42)
typewriter(2, text, 0.1, 0.4, 0.8, 0.25, 1.0, 1.0, 1.0, 1.0, 0.01)

gs.sleep(2.0)
wait_input(0.0, [1, 2])

gateway.close()
