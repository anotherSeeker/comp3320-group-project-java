# the sunken castle (comp3320-group-project-java)
Java rewrite of c++ engine

Very basic implementation of particle effects.
- In "ParticlesDemoScene" and "AssetManager" files you may need to alter the pathPrefix to get it working on your machine.  

## languages
- java

## libraries
- LWJGL
- Dominion ECS

## tooling
- gradle

## style guide
SCREAMING_SNAKE_CASE for constants and macros.
camelCase for methods, functions, variables and class properties.
PascalCase for everything else (classes).

Services provided to native scripts such as those used for Asset management or Input management should be in PascalCase.
The methods provided by those services should also be in PascalCase.
