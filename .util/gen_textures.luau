--!strict
local fs = require("@vara/fs")
local media = require("@vara/media")
local noise = require("./noise")

const WIDTH = 512;
const HEIGHT = 512;
const BITDEPTH = 4

local perlinTex = media.Image(buffer.create(WIDTH * HEIGHT * BITDEPTH * 4)):Dimensions(WIDTH, HEIGHT)
noise.genPerlinTexture(perlinTex,4,2.0,0.5,0.015)
perlinTex.Channels = 1

local curlTex = media.Image(buffer.create(WIDTH * HEIGHT * BITDEPTH * 4)):Dimensions(WIDTH, HEIGHT)
noise.genCurlTexture(curlTex,perlinTex)
curlTex.Channels = 3

fs.File("app/assets/textures/internal/perlin.png"):Write(perlinTex:Resize(WIDTH, HEIGHT,"BILINEAR"):Encode("PNG"))
fs.File("app/assets/textures/internal/curl.png"):Write(curlTex:Resize(WIDTH,HEIGHT,"BILINEAR"):Encode("PNG"))

print("\x1b[1;32mcreated textures successfully!\x1b[0m")