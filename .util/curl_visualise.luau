--!strict
local colour = require("@vara/colour")
local fs = require("@vara/fs")
local media = require("@vara/media")

function traverseVectorField(x0: number,y0: number,image: Image,vectorField: Image)
    local x = x0
    local y = y0

    const MAX_ATTEMPTS = 100
    local attempts = 0

    while true do
        local grad = vectorField:GetPixel(x, y)
        local dx = math.map(grad.Red,0,1,-1,1)
        local dy = math.map(grad.Green,0,1,-1,1)
 
        x += dx * 20 // 1
        y += dy * 20 // 1

        attempts += 1

        if attempts > MAX_ATTEMPTS then break end

        if x < 0 or y < 0 or x > image.Width - 1 or y > image.Height - 1 then 
            x = math.clamp(x,0,image.Width - 1)
            y = math.clamp(x,0,image.Height - 1)

            image:SetPixel(x, y,colour.New(1,1,1,1))

            break 
        end

        image:SetPixel(x, y,colour.New(1,1,1,1))
    end
end

local curlTex = media.Image("app/assets/textures/internal/curl.png")

const WIDTH = curlTex.Height
const HEIGHT = curlTex.Width

local visualiser = media.Image(buffer.create(WIDTH * HEIGHT * 4 * 4))
:Dimensions(curlTex.Width, curlTex.Height)
:Resize(WIDTH * 4, HEIGHT * 4)

curlTex:Resize(WIDTH * 4, HEIGHT * 4)

local spacing = 20

for y = 0,curlTex.Height - 1,spacing do
    for x = 0,curlTex.Width - 1,spacing do
        traverseVectorField(x,y,visualiser,curlTex)
    end
end



visualiser:Convolve(5, 5,{
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
    :Resize(WIDTH,HEIGHT)

visualiser.Channels = 1

fs.File(".util/curl_debug.png"):Write(visualiser:Resize(WIDTH,HEIGHT):Encode("PNG"))
