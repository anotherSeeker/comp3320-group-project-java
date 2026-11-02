--!strict
-- perlin noise
local fs = require("@vara/fs")
local media = require("@vara/media")
local noise = require("./noise")

const WIDTH = 512;
const HEIGHT = 512;
const BITDEPTH = 4

local perlinTex = media.Image(buffer.create(WIDTH * HEIGHT * BITDEPTH * 4)):Dimensions(WIDTH, HEIGHT)
noise.genPerlinTexture(perlinTex,4,0.5,2,0.15)
perlinTex.Channels = 1

local curlTex = media.Image(buffer.create(WIDTH * HEIGHT * BITDEPTH * 4)):Dimensions(WIDTH, HEIGHT):Resize(WIDTH * 4, HEIGHT * 4,"BILINEAR")
noise.genCurlTexture(curlTex,perlinTex:Resize(WIDTH * 4, HEIGHT * 4,"BILINEAR"))
curlTex.Channels = 1

curlTex:Convolve(5, 5,{
    1/256,4/256,6/256,4/256,1/256,
    4/256,16/256,24/256,16/256,4/256,
    6/256,24/256,36/256,24/256,6/256,
    4/256,16/256,24/256,16/256,4/256,
     1/256,4/256,6/256,4/256,1/256,
}):Convolve(5, 5,{
    1/256,4/256,6/256,4/256,1/256,
    4/256,16/256,24/256,16/256,4/256,
    6/256,24/256,36/256,24/256,6/256,
    4/256,16/256,24/256,16/256,4/256,
     1/256,4/256,6/256,4/256,1/256,
})


fs.File("app/assets/textures/internal/perlin.png"):Write(perlinTex:Resize(WIDTH, HEIGHT,"BILINEAR"):Encode("PNG"))
fs.File("app/assets/textures/internal/curl.png"):Write(curlTex:Resize(WIDTH,HEIGHT,"BILINEAR"):Encode("PNG"))

print("\x1b[1;32mcreated textures successfully!\x1b[0m")