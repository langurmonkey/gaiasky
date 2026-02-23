# This script changes the star texture a few times

# Created by Toni Sagrista

from py4j.clientserver import ClientServer, JavaParameters

gateway = ClientServer(java_parameters=JavaParameters(auto_convert=True))
gs = gateway.entry_point

gs.cameraStop()
gs.setStarBrightness(40)
gs.setStarSize(11.0)
gs.setStarMinOpacity(0.35)

ntextures = 4

for i in range(ntextures):
    texi = (i % ntextures) + 1
    gs.setStarTextureIndex(texi)
    gs.log("Star texture changed to %d" % texi)
    gs.sleep(2.0)

gs.setStarTextureIndex(4)
gs.setStarBrightness(21.0)
gs.setStarSize(3.0)


# close connection
gateway.close()
