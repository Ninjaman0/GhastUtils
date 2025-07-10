# GhastUtils Plugin

A comprehensive Minecraft utility plugin for Paper/Spigot servers that provides multipliers, custom armor, selling systems, crafting mechanics, and interactive blocks.

## Features

### 🔢 Multiplier System
- **Global Multipliers**: Server-wide multiplier effects
- **Player Boosters**: Temporary multiplier boosts for individual players
- **Pet Multipliers**: Integration with RivalPets for pet-based multipliers
- **Armor Multipliers**: Custom armor pieces that provide multiplier bonuses
- **Event Multipliers**: Time-limited server events with multiplier bonuses

### 🛡️ Custom Armor System
- Create custom armor pieces with unique multipliers
- Support for custom model data and textures
- Player head support with custom textures
- Colored leather armor support
- Permission-based armor restrictions
- Particle effects for equipped armor

### 💰 Advanced Selling System
- Sell items with multiplier bonuses
- Interactive sell GUI
- Auto-sell functionality
- Custom item pricing
- NBT data support for unique items
- Fuzzy matching for item identification

### 🔨 Custom Crafting System
- Create custom items with unique recipes
- Support for custom ingredients
- Recipe validation and circular dependency prevention
- Custom model data and NBT support
- Permission-based crafting restrictions
- Auto-crafting functionality

### 🧱 Interactive Blocks
- Create custom interactive blocks
- Command execution on interaction
- Cooldown system
- Permission-based access
- Multiple interaction types (left-click, right-click, shift-click)

### 🤖 Automation Features
- **Auto-Sell**: Automatically sell items every 3 seconds
- **Auto-Craft**: Automatically craft items from inventory
- **Compactor**: GUI-based item compacting system

## Installation

1. Download the latest release
2. Place the JAR file in your server's `plugins` folder
3. Restart your server
4. Configure the plugin using the generated configuration files

## Dependencies

### Required
- **Paper/Spigot**: 1.21.4+
- **EssentialsX**: For economy integration

### Optional
- **PlaceholderAPI**: For placeholder support
- **RivalPets**: For pet multiplier integration
- **Vault**: For economy API support

## Configuration

### Main Configuration (`config.yml`)
```yaml
# Multiplier settings
multiplier:
  global: 1.0
  min: 1.0
  multiply_effects: false
  add_effects: true

# Armor multiplier settings
armor-multipliers:
  enabled: true
  particles:
    enabled: true
    type: WITCH
    count: 15
    radius: 0.5
```

### Custom Armor (`config.yml`)
```yaml
armor-multipliers:
  pieces:
    diamond_helmet:
      material: DIAMOND_HELMET
      name: "&bDiamond Helmet"
      lore:
        - "&7Increases your multiplier by &b10%"
      multiplier: 0.1
      permission: ghastutils.armor.diamond
```

### Custom Crafting (`crafting.yml`)
```yaml
# Custom ingredients
ingredients:
  grinding_wheat:
    item-name: "&5Wheat&3!!"
    lore:
      - "Just normal wheat"
    custom-model-data: 21245

# Custom items
example_item:
  itemname: "&6Example Item"
  material: DIAMOND_SWORD
  recipe:
    1: grinding_wheat:30
    2: DIAMOND:1
```

### Sellable Items (`sell.yml`)
```yaml
items:
  diamond:
    material: DIAMOND
    base_price: 100.0
    permission: "NONE"
```

### Interactive Blocks (`block_commands.yml`)
```yaml
blocks:
  teleport_pad:
    name: "&b&lTeleport Pad"
    material: LIGHT_BLUE_GLAZED_TERRACOTTA
    commands:
      RIGHT_CLICK:
        - "console: spawn %player%"
```

## Commands

### Main Commands
- `/gutil` - Main plugin command
- `/gutil help` - Show help menu
- `/gutil reload` - Reload plugin configuration

### Multiplier Commands
- `/gutil multiplier` - View your multiplier breakdown
- `/gutil booster give <player> <multiplier> <duration>` - Give a booster
- `/gutil event create <id> <name> <value> [duration]` - Create event multiplier

### Selling Commands
- `/sell` - Sell items in inventory
- `/sell gui` - Open sell GUI
- `/gutil autosell` - Toggle auto-selling

### Crafting Commands
- `/gutil crafting register <id>` - Register held item as custom item
- `/gutil crafting editor <id>` - Open recipe editor
- `/gutil crafting view <id>` - View recipe
- `/gutil autocraft` - Toggle auto-crafting
- `/gutil compactor` - Open compactor GUI

### Armor Commands
- `/gutil armor give <player> <id> [amount]` - Give custom armor
- `/gutil armor list` - List available armor pieces

### Block Commands
- `/gutil block get <id>` - Get custom block item
- `/gutil block set <id>` - Set block at target location

## Permissions

### Admin Permissions
- `ghastutils.admin` - Full admin access
- `ghastutils.admin.reload` - Reload configuration
- `ghastutils.bypass.enchant` - Bypass enchanting restrictions

### Feature Permissions
- `ghastutils.multiplier` - View multipliers
- `ghastutils.sell` - Use sell system
- `ghastutils.sell.gui` - Use sell GUI
- `ghastutils.sell.autosell` - Use auto-sell
- `ghastutils.crafting` - View crafting recipes
- `ghastutils.crafting.admin` - Manage recipes
- `ghastutils.armor.admin` - Manage armor
- `ghastutils.block.admin` - Manage blocks

## PlaceholderAPI Support

- `%ghastutils_multiplier_total%` - Player's total multiplier
- `%ghastutils_booster_timeleft%` - Remaining booster time
- `%ghastutils_booster_active%` - Whether booster is active
- `%ghastutils_pet_active%` - Whether pet multiplier is active
- `%ghastutils_event_active%` - Whether events are active

## API Usage

```java
// Get player's total multiplier
MultiplierManagerinter api = multiplierProvider.getAPI();
double multiplier = api.getTotalMultiplier(player);
```

## Support

For support, bug reports, or feature requests, please contact the plugin developer.

## Version

Current Version: 2.5
Minecraft Version: 1.21.4
API Version: 1.21