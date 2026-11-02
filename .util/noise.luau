--!strict
-- perlin noise
local colour = require("@vara/colour")

local module = {}

-- map 2d plane to torus to prevent tiling.
function seamlessPerlin(x: number,y: number,width: number,height: number,frequency: number)
    local u = (x/width) * math.tau * frequency
    local v = (y/height) * math.tau * frequency

    local radiusA = 6.0
    local radiusB = 2.0

    local nx = (radiusA + radiusB * math.cos(v)) * math.cos(u)
    local ny = (radiusA + radiusB * math.cos(v)) * math.sin(u)
    local nz = radiusB * math.sin(v)

    return math.noise(nx,ny,nz)
end

function perlinNoise(x: number,y: number,width: number,height: number,octaves: number,lacunarity: number,persistence: number,scale: number)
    local result: number = 0.0
    local max_amplitude = 0
    local amplitude = 1
    local frequency = scale

    for i = 1,octaves do
        local value = seamlessPerlin(x,y,width,height,frequency)

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

            local value = perlinNoise(x,y,image.Width,image.Height,octaves,lacunarity,persistence,scale)
            local pixel = colour.New(value,value,value,1)

            image:SetPixel(x, y, pixel)
        end
    end
end

function wrap(value: number,size: number)
    return value % size
end

function module.genCurlTexture(image: Image, perlinImage: Image)
    local epsilon = 1

    for y = 0,image.Height - 1 do
        for x = 0,image.Width - 1 do
       
            local top = perlinImage:GetPixel(x, wrap(y-epsilon, image.Height)).Red
            local bottom = perlinImage:GetPixel(x, wrap(y+epsilon, image.Height)).Red

            local left = perlinImage:GetPixel(wrap(x-epsilon, image.Width),y).Red
            local right = perlinImage:GetPixel(wrap(x+epsilon, image.Width),y).Red

            local dx = (right - left) / (2 * epsilon)
            local dy = (bottom - top) / (2 * epsilon)
            
            local vx = dy
            local vy = -dx

            local pixel = colour.New(
                math.clamp(vx,-1,1) * 0.5 + 0.5, 
                math.clamp(vy,-1,1) * 0.5 + 0.5, 
                0,
                1
            )

            image:SetPixel(x, y, pixel)
        end
    end
end

return module
