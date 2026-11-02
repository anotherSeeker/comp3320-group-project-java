-- perlin noise
local fs = require("@vara/fs")
local media = require("@vara/media")
local colour = require("@vara/colour")

local output = fs.File(".util/perlin.png")

const WIDTH = 512;
const HEIGHT = 512;
const BITDEPTH = 4

local image = media.Image(buffer.create(WIDTH * HEIGHT * BITDEPTH * 4))
image.Channels = 1
print(image.Width,image.Height,image.Channels)
image:Dimensions(WIDTH, HEIGHT)

for y = 0,HEIGHT - 1 do
    for x = 0,WIDTH - 1 do

        local perlin = math.map(math.noise(x * 0.1,y * 0.1,12345),-1,1,0,1)
        local pixel = colour.New(perlin,perlin,perlin,1)

        image:SetPixel(x, y, pixel)
    end
end

output:Write(image:Encode("PNG"))

