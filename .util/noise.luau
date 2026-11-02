--!strict
-- perlin noise
local colour = require("@vara/colour")

local module = {}

function perlinNoise(x: number,y: number,octaves: number,lacunarity: number,persistence: number,scale: number)
    local result: number = 0.0
    local max_amplitude = 0
    local amplitude = 1
    local frequency = scale

    for i = 1,octaves do
        local value = math.noise(x * frequency,y * frequency,12345)

        result += value * amplitude
        max_amplitude += amplitude
        amplitude *= persistence
        frequency *= lacunarity
    end

    result /= max_amplitude
    result = math.map(result,-1,1,0,1)
    return result
end

function module.genPerlinTexture(image: Image,octaves: number,lacunarity: number,persistence: number,scale: number)
    for y = 0,image.Height - 1 do
        for x = 0,image.Width - 1 do

            local value = perlinNoise(x,y,octaves,lacunarity,persistence,scale)
            local pixel = colour.New(value,value,value,1)

            image:SetPixel(x, y, pixel)
        end
    end
end

function module.genCurlTexture(image: Image, perlinImage: Image)
    local epsilon = 1

    for y = 0,image.Height - 1 do
        for x = 0,image.Width - 1 do
       
            local top = math.map(perlinImage:GetPixel(x, math.max(y-epsilon,0)).Red,0,1,-1,1)
            local bottom = math.map(perlinImage:GetPixel(x, math.min(y+epsilon,image.Height - 1)).Red,0,1,-1,1)

            local left = math.map(perlinImage:GetPixel(math.max(x-epsilon,0),y).Red,0,1,-1,1)
            local right = math.map(perlinImage:GetPixel(math.min(x+epsilon,image.Width - 1),y).Red,0,1,-1,1)

            local dx = (right - left) / (2 * epsilon)
            local dy = (bottom - top) / (2 * epsilon)
            
            local vx = math.map(dy,-1,1,0,1)
            local vy = math.map(-dx,-1,1,0,1)

            local pixel = colour.New(vx, vy, 0,1)

            image:SetPixel(x, y, pixel)
        end
    end
end

return module
