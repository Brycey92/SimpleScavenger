package dk.xakeps.simplescavenger;

import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.effect.sound.SoundType;
import org.spongepowered.api.effect.sound.SoundTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.entity.HarvestEntityEvent;
import org.spongepowered.api.event.entity.living.humanoid.player.RespawnPlayerEvent;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.serializer.TextSerializers;
import org.spongepowered.api.text.translation.locale.Locales;

import javax.inject.Inject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

@Plugin(id = "simplescavenger",
        name = "Simple Scavenger",
        version = "2.0",
        description = "Keeps inventory and xp for players with permissions",
        url = "https://spongeapi.com",
        authors = "Xakep_SDK, Brycey92")
public class SimpleScavenger {

    private CommentedConfigurationNode rootNode;
    
    @Inject
    @DefaultConfig(sharedRoot = true)
    ConfigurationLoader<CommentedConfigurationNode> loader;
    
    @Inject
    @ConfigDir(sharedRoot = true)
    Path configDir;

    private boolean respawnSoundEnabled;
    private double respawnSoundVolume;
    private SoundType respawnSound;

    private boolean deathSoundEnabled;
    private double deathSoundVolume;
    private SoundType deathSound;

    private boolean respawnMessageEnabled;
    private String respawnDefaultMessage;

    private boolean deathMessageEnabled;
    private String deathInventoryMessage;
    private String deathExperienceMessage;
    private String deathBothMessage;

    /**
	 * onInit Method. Called on Plugin Load
	 * @param event
	 */
	@Listener
    public void onInit(GameInitializationEvent event) throws IOException {
        loadConfig();

        CommandSpec spec = CommandSpec.builder()
                .permission("simplescavenger.reload")
                .description(Text.of("Reloads plugin"))
                .extendedDescription(Text.of("Reloads plugin configuration"))
                .executor((src, args) -> {
                    try {
                        loadConfig();
                        Text successMsg = Text.of(TextColors.GRAY, '[', TextColors.GOLD, "SimpleScavenger", TextColors.GRAY, ']',
                                TextColors.WHITE, " Plugin reloaded!");
                        src.sendMessage(successMsg);
                        return CommandResult.success();
                    } catch (IOException e) {
                        Text errorMsg = Text.of(TextColors.GRAY, '[', TextColors.GOLD, "SimpleScavenger", TextColors.GRAY, ']',
                                TextColors.WHITE, " Error while reloading plugin. Check console.");
                        src.sendMessage(errorMsg);
                        return CommandResult.empty();
                    }
                })
                .build();
        Sponge.getCommandManager().register(this, spec, "simplescavenger", "scavenger");
    }

    @Listener
    public void onPlayerDeath(HarvestEntityEvent.TargetPlayer event, @Getter("getTargetEntity") Player player) {
    	//cache the permissions for a moment so we only look them up once from the permissions system per death
    	boolean use = player.hasPermission("simplescavenger.use");
    	boolean keepInventory = player.hasPermission("simplescavenger.keepInventory");
    	boolean keepExperience = player.hasPermission("simplescavenger.keepExperience");
    	
        if(use || keepInventory || keepExperience) {
        	if(use || keepInventory)
        		event.setKeepsInventory(true);
        	if(use || keepExperience)
        		event.setKeepsLevel(true);
        	
            if (deathMessageEnabled) {
            	String msg = "";
            	
            	if(use || (keepInventory && keepExperience))
            		msg = rootNode.getNode("messages", "death", "both", player.getLocale().getLanguage()).getString(deathBothMessage);
            	else if(keepInventory)
                	msg = rootNode.getNode("messages", "death", "inventory", player.getLocale().getLanguage()).getString(deathInventoryMessage);
            	else if(keepExperience)
                	msg = rootNode.getNode("messages", "death", "experience", player.getLocale().getLanguage()).getString(deathExperienceMessage);
            	
                Text text = TextSerializers.FORMATTING_CODE.deserialize(msg);
                player.sendMessage(text);
            }

            if (deathSoundEnabled) {
                player.playSound(deathSound, player.getLocation().getPosition(), deathSoundVolume);
            }
        }
    }

    @Listener
    public void onPlayerRespawn(RespawnPlayerEvent event) {
        Player player = event.getTargetEntity();
        if (respawnMessageEnabled) {
            String msg = rootNode.getNode("messages", "respawn", player.getLocale().getLanguage())
                    .getString(respawnDefaultMessage);
            Text text = TextSerializers.FORMATTING_CODE.deserialize(msg);
            player.sendMessage(text);
        }

        if (respawnSoundEnabled) {
            player.playSound(respawnSound, player.getLocation().getPosition(), respawnSoundVolume);
        }
    }

    private void loadConfig() throws IOException {
    	File configFile = new File(configDir.toUri());
    	if (!configFile.exists())
			configFile.createNewFile();
    	
    	saveDefaultConfig();
        rootNode = loader.load();
		
        respawnSoundEnabled = rootNode.getNode("sounds", "respawn", "enabled").getBoolean();
        respawnSound = SoundType.of(rootNode.getNode("sounds", "respawn", "sound").getString());
        respawnSoundVolume = rootNode.getNode("sounds", "respawn", "volume").getDouble();

        deathSoundEnabled = rootNode.getNode("sounds", "death", "enabled").getBoolean();
        deathSound = SoundType.of(rootNode.getNode("sounds", "death", "sound").getString());
        deathSoundVolume = rootNode.getNode("sounds", "death", "volume").getDouble();
        
        String defLang = rootNode.getNode("messages", "defaultLang").getString();
        
        respawnMessageEnabled = rootNode.getNode("messages", "respawn", "enabled").getBoolean();
        respawnDefaultMessage = rootNode.getNode("messages", "respawn", defLang).getString();

        deathMessageEnabled = rootNode.getNode("messages", "death", "enabled").getBoolean();
        deathInventoryMessage = rootNode.getNode("messages", "death", "inventory", defLang).getString();
        deathExperienceMessage = rootNode.getNode("messages", "death", "experience", defLang).getString();
        deathBothMessage = rootNode.getNode("messages", "death", "both", defLang).getString();
    }
    
    private void saveDefaultConfig() throws IOException {
    	this.rootNode = loader.load();
    	
    	if(rootNode.getNode("sounds", "respawn", "enabled").isVirtual())
        	rootNode.getNode("sounds", "respawn", "enabled").setValue(true);
    	if(rootNode.getNode("sounds", "respawn", "sound").isVirtual())
    		rootNode.getNode("sounds", "respawn", "sound").setValue(SoundTypes.ENTITY_PLAYER_LEVELUP.getId());
    	if(rootNode.getNode("sounds", "respawn", "volume").isVirtual())
    		rootNode.getNode("sounds", "respawn", "volume").setValue(1.0);
    	
    	if(rootNode.getNode("sounds", "death", "enabled").isVirtual())
    		rootNode.getNode("sounds", "death", "enabled").setValue(true);
    	if(rootNode.getNode("sounds", "death", "sound").isVirtual())
    		rootNode.getNode("sounds", "death", "sound").setValue(SoundTypes.BLOCK_ANVIL_LAND.getId());
    	if(rootNode.getNode("sounds", "death", "volume").isVirtual())
    		rootNode.getNode("sounds", "death", "volume").setValue(1.0);
    	
    	if(rootNode.getNode("messages", "defaultLang").isVirtual())
    		rootNode.getNode("messages", "defaultLang").setValue(Locales.EN_US.getLanguage()).setComment("Can be either en or ru by default, but you can add other languages.");
    	
    	if(rootNode.getNode("messages", "respawn", "enabled").isVirtual())
    		rootNode.getNode("messages", "respawn", "enabled").setValue(true);
        if(rootNode.getNode("messages", "respawn", Locales.EN_US.getLanguage()).isVirtual())
        	rootNode.getNode("messages", "respawn", Locales.EN_US.getLanguage()).setValue("&7[&6SimpleScavenger&7]&f Respawned!");
        if(rootNode.getNode("messages", "respawn", Locales.RU_RU.getLanguage()).isVirtual())
        	rootNode.getNode("messages", "respawn", Locales.RU_RU.getLanguage()).setValue("&7[&6SimpleScavenger&7]&f Вы возрождены!");
        
        if(rootNode.getNode("messages", "death", "enabled").isVirtual())
        	rootNode.getNode("messages", "death", "enabled").setValue(true);
        
        if(rootNode.getNode("messages", "death", "inventory", Locales.EN_US.getLanguage()).isVirtual())
        	rootNode.getNode("messages", "death", "inventory", Locales.EN_US.getLanguage()).setValue("&7[&6SimpleScavenger&7]&f Your inventory was saved!");
        if(rootNode.getNode("messages", "death", "inventory", Locales.RU_RU.getLanguage()).isVirtual())
        	rootNode.getNode("messages", "death", "inventory", Locales.RU_RU.getLanguage()).setValue("&7[&6SimpleScavenger&7]&f Ваш инвентарь был сохранён!");
        
        if(rootNode.getNode("messages", "death", "experience", Locales.EN_US.getLanguage()).isVirtual())
        	rootNode.getNode("messages", "death", "experience", Locales.EN_US.getLanguage()).setValue("&7[&6SimpleScavenger&7]&f Your experience was saved!");
        if(rootNode.getNode("messages", "death", "experience", Locales.RU_RU.getLanguage()).isVirtual())
        	rootNode.getNode("messages", "death", "experience", Locales.RU_RU.getLanguage()).setValue("&7[&6SimpleScavenger&7]&f Ваш опыт был сохранён!");
        
        if(rootNode.getNode("messages", "death", "both", Locales.EN_US.getLanguage()).isVirtual())
        	rootNode.getNode("messages", "death", "both", Locales.EN_US.getLanguage()).setValue("&7[&6SimpleScavenger&7]&f Your inventory and experience were saved!");
        if(rootNode.getNode("messages", "death", "both", Locales.RU_RU.getLanguage()).isVirtual())
        	rootNode.getNode("messages", "death", "both", Locales.RU_RU.getLanguage()).setValue("&7[&6SimpleScavenger&7]&f Ваш инвентарь и опыт были сохранёны!");
        
        loader.save(rootNode);
    }
}
