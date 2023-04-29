# Battalion

Battalion is a replication of [Battalion: Nemesis](https://kongregate.fandom.com/wiki/Battalion:_Nemesis), a strategy game where you maneuver military units on a grid and the goal is to defeat the enemy units. The game is turn-based and each unit can move a certain number of spaces in a turn (adjusted for terrain type). There is a variety of units types such as Infantry soldiers, tanks, planes, ships, submarines and more, each with its own propertices: attack range, movement range, which units it can attack, which terrain types it can pass through, ect. By conquering different building, a player can unlock new technologies and units types, increase his income, unlock strategically located unit factories, ect.
In addition to the original game levels, the player can build and play his own designed level via `MainMenu -> LevelBuilder`.

The code is written in `Java` using the Swing GUI library. The units stats were taken from [Fandom](https://kongregate.fandom.com/wiki/Units_of_Battalion_Versions) and (most of) the images were taken from [spriters-resource](https://www.spriters-resource.com/pc_computer/battalionnemesis/). The in-game text such as units or buildings description was generated using ChatGPT.


### Installation

Clone and build the game with the following commands:
```bash
git clone https://github.com/barakugav/battalion.git
cd battalion
mvn clean compile assembly:single
```
Run the game using the following command:
```bash
java -jar .\target\bugav-battalion-1.0-SNAPSHOT-jar-with-dependencies.jar
```

<div align="center">
<img src="https://github.com/barakugav/battalion/blob/master/doc/readme_img/main_menu.png?raw=true" alt="main menu"/>
</div>
<div align="center">
<img src="https://github.com/barakugav/battalion/blob/master/doc/readme_img/level.png?raw=true" alt="level 10"/>
</div>
<div align="center">
<img src="https://github.com/barakugav/battalion/blob/master/doc/readme_img/factory.png?raw=true" alt="factory menu"/>
</div>
