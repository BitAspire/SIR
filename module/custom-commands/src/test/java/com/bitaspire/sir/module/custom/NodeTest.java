package com.bitaspire.sir.module.custom;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class NodeTest {

    private static final String TREE = String.join("\n",
            "messages: [ 'root' ]",
            "arguments:",
            "  amount:",
            "    type: INTEGER",
            "    min: 1",
            "    max: 64",
            "  give:",
            "    type: LITERAL",
            "    aliases: [ g ]",
            "    arguments:",
            "      color:",
            "        type: CHOICE",
            "        choices: [ red, blue ]",
            "        commands: [ 'say {color}' ]",
            "  ratio:",
            "    type: NUMBER",
            "    max: 1",
            "  flag:",
            "    type: BOOLEAN",
            "  text:",
            "    type: STRING",
            "    greedy: true",
            "    arguments:",
            "      ignored:",
            "        type: LITERAL",
            "  broken:",
            "    type: CHOICE",
            "    min: 5",
            "    max: 1"
    );

    private final List<String> warnings = new ArrayList<>();

    private static Map<String, String> rollAll(Node node, Map<String, String> values, Random random) {
        node.getRandomValues().values().forEach(value -> value.roll(values, random, 1000));
        return values;
    }

    private static String roll(Node node, String key, Random random) {
        return rollAll(node, new LinkedHashMap<>(), random).get(key);
    }

    private Node root() throws InvalidConfigurationException {
        YamlConfiguration yml = new YamlConfiguration();
        yml.loadFromString(TREE);
        return new Node("test", Node.Type.LITERAL, yml, warnings);
    }

    @Test
    void literalsAreMatchedBeforeTypedArguments() throws Exception {
        Node root = root();

        assertEquals("give", root.find(null, "give").getName());
        assertEquals("give", root.find(null, "G").getName());
        assertEquals("amount", root.find(null, "5").getName());
    }

    @Test
    void typedArgumentsAreTriedInOrder() throws Exception {
        Node root = root();

        assertEquals("ratio", root.find(null, "0.5").getName());
        // Out of the INTEGER and NUMBER ranges, so it falls through to the STRING.
        assertEquals("text", root.find(null, "100").getName());
        assertEquals("flag", root.find(null, "TRUE").getName());
        assertEquals("text", root.find(null, "anything").getName());
    }

    @Test
    void rangesAndChoicesAreValidated() throws Exception {
        Node root = root();
        Node amount = root.find(null, "1"), color = root.find(null, "give").find(null, "red");

        assertTrue(amount.matches(null, "64"));
        assertFalse(amount.matches(null, "65"));
        assertFalse(amount.matches(null, "0"));
        assertFalse(amount.matches(null, "1.5"));

        assertEquals("color", color.getName());
        assertTrue(color.matches(null, "BLUE"));
        assertFalse(color.matches(null, "green"));
        assertNull(root.find(null, "give").find(null, "green"));
    }

    @Test
    void actionsAndSyntax() throws Exception {
        Node root = root();
        Node give = root.find(null, "give");

        assertTrue(root.hasActions());
        assertFalse(give.hasActions());
        assertTrue(give.find(null, "red").hasActions());

        assertEquals("[<amount>|give|<ratio>|<flag>|<text>|<broken>]", root.syntax());
        assertEquals("<<color>>", give.syntax());
        assertEquals("red", give.find(null, "red").suggest(null).get(0));
    }

    @Test
    void greedyArgumentsDropTheirChildrenWithAWarning() throws Exception {
        Node text = root().find(null, "anything");

        assertTrue(text.isGreedy());
        assertTrue(text.getChildren().isEmpty());
        assertTrue(warnings.stream().anyMatch(w -> w.contains("'text' is greedy")));
        assertTrue(warnings.stream().anyMatch(w -> w.contains("'broken' is a CHOICE without choices")));
        assertTrue(warnings.stream().anyMatch(w -> w.contains("'broken' has a min greater than its max")));
    }

    private Node load(String resource) throws Exception {
        YamlConfiguration yml;
        try (java.io.Reader reader = new java.io.InputStreamReader(
                getClass().getClassLoader().getResourceAsStream(resource),
                java.nio.charset.StandardCharsets.UTF_8)) {
            yml = YamlConfiguration.loadConfiguration(reader);
        }
        return new Node("test", Node.Type.LITERAL, yml, warnings);
    }

    @Test
    void exampleFileLoadsWithoutWarnings() throws Exception {
        Node root = load("commands/example.yml");

        assertTrue(warnings.isEmpty(), warnings::toString);
        assertEquals("value", root.find(null, "speed").find(null, "0.5").getName());
        assertNull(root.find(null, "speed").find(null, "2"));
        assertTrue(root.find(null, "say").find(null, "hi").isGreedy());

        Node roll = root.find(null, "roll"), reward = root.find(null, "reward");
        int amount = Integer.parseInt(roll(roll, "amount", new Random(3)));
        assertTrue(amount >= 1 && amount <= 6);
        assertEquals(3, reward.getOutcomes().size());
        assertTrue(reward.hasActions());
    }

    @Test
    void clearChatFileLoadsWithoutWarnings() throws Exception {
        Node root = load("commands/clear-chat.yml");

        assertTrue(warnings.isEmpty(), warnings::toString);
        assertEquals("@a", root.find(null, "@A").getName());
        assertEquals(root.getMessages(), root.find(null, "help").getMessages(), "the help anchor is shared");
        assertFalse(root.getChildren().get(2).isExact());
    }

    @Test
    void printFileLoadsWithoutWarnings() throws Exception {
        Node root = load("commands/print.yml");
        assertTrue(warnings.isEmpty(), warnings::toString);

        Node chat = root.find(null, "chat").find(null, "perm:vip");
        assertEquals("centered", chat.find(null, "centered").getName());
        assertEquals("default", chat.find(null, "mixed").getName());
        assertTrue(chat.find(null, "hello").isGreedy());

        Node title = root.find(null, "title").find(null, "@a");
        assertEquals("seconds", title.find(null, "5").getName());
        assertEquals("default", title.find(null, "default").getName());
    }

    @Test
    void bundledFunFilesLoadWithoutWarnings() throws Exception {
        for (String name : new String[] {"ball", "anon"}) {
            Node root = load("commands/" + name + ".yml");
            assertTrue(warnings.isEmpty(), name + ": " + warnings);
            assertTrue(root.getChildren().get(0).isGreedy());
        }

        Set<String> answers = new HashSet<>();
        Random random = new Random(5);
        Node question = load("commands/ball.yml").getChildren().get(0);
        for (int i = 0; i < 500; i++) answers.add(roll(question, "answer", random));

        assertEquals(17, answers.size());
        assertTrue(answers.contains("It's not clear yet, try again"));
    }

    @Test
    void randomValueListsTakeWeightsAndChances() throws Exception {
        YamlConfiguration yml = new YamlConfiguration();
        yml.loadFromString(String.join("\n",
                "random-values:",
                "  color: [ red, red, green ]",
                "  never: [ '[weight:0]x' ]",
                "  old:",
                "    common: 9",
                "  empty: []"));

        Node root = new Node("test", Node.Type.LITERAL, yml, warnings);
        assertEquals(3, warnings.size(), warnings::toString);
        assertEquals(Collections.singleton("color"), root.getRandomValues().keySet());

        int red = 0;
        Random random = new Random(7);
        for (int i = 0; i < 3000; i++)
            if (roll(root, "color", random).equals("red")) red++;
        assertTrue(red > 1800 && red < 2200, "repeated entries keep their share: " + red);
    }

    @Test
    void chancesAreExactAndTheRestIsSharedByWeight() {
        List<String> values = new ArrayList<>(), warned = new ArrayList<>();
        List<Double> weights = new ArrayList<>();

        assertTrue(RandomValue.pool(Arrays.asList("[chance:5%]Sword", "[CHANCE:20]Diamond", "[weight:2]Iron", "Coal"),
                values, new ArrayList<>(), weights, "reward", warned));
        assertEquals(Arrays.asList("Sword", "Diamond", "Iron", "Coal"), values);
        assertEquals(Arrays.asList(5.0, 20.0, 50.0, 25.0), weights);
        assertTrue(warned.isEmpty());
    }

    @Test
    void missingPercentPicksNothingAndExtraIsScaled() {
        List<String> values = new ArrayList<>(), warned = new ArrayList<>();
        List<Double> weights = new ArrayList<>();

        RandomValue.pool(Collections.singletonList("[chance:10%]Emerald"), values, new ArrayList<>(), weights, "drop", warned);
        assertEquals(Arrays.asList("Emerald", ""), values);
        assertEquals(Arrays.asList(10.0, 90.0), weights);

        values.clear();
        weights.clear();
        RandomValue.pool(Arrays.asList("[chance:150]a", "[chance:50]b", "c"), values, new ArrayList<>(), weights, "over", warned);
        assertEquals(Arrays.asList(75.0, 25.0, 0.0), weights);
        assertEquals(1, warned.size());
    }

    @Test
    void everyBundledFileLoadsWithActionsAndNoWarnings() throws Exception {
        String[] names = {"example", "clear-chat", "print", "dice", "ball", "anon", "coin", "me", "broadcast", "try", "ping", "brand", "daily"};
        for (String name : names) {
            Node root = load("commands/" + name + ".yml");
            assertTrue(warnings.isEmpty(), name + ": " + warnings);
            assertTrue(root.hasActions() || !root.getChildren().isEmpty(), name);
        }

        Set<String> sides = new HashSet<>();
        Node coin = load("commands/coin.yml");
        Random random = new Random(9);
        for (int i = 0; i < 2000; i++) sides.add(roll(coin, "side", random));
        assertEquals(new HashSet<>(Arrays.asList("heads", "tails", "edge")), sides);
    }

    @Test
    void tryShowsTheSameResultInBothConditions() throws Exception {
        Node action = load("commands/try.yml").find(null, "jumps");
        Map<String, String> values = new LinkedHashMap<>();
        values.put("player", "Steve");
        values.put("action", "jumps");
        values.put("percent", "72");

        String line = Lines.parse(action.getMessages().get(0), 1000).getText();
        assertEquals("&a☺ &7Steve &fjumps &a72%", Lines.evaluate(Lines.apply(line, values), new Random()));
    }

    @Test
    void durationsBecomeSeconds() {
        assertEquals(90, Node.seconds("90"));
        assertEquals(30, Node.seconds("30s"));
        assertEquals(5400, Node.seconds("1h30m"));
        assertEquals(172800, Node.seconds("2D"));
        assertEquals(604800, Node.seconds("1w"));
        assertEquals(-1, Node.seconds("soon"));
        assertEquals(-1, Node.seconds("1h abc"));
    }

    @Test
    void costIsReadFromAnyArgument() throws Exception {
        YamlConfiguration yml = new YamlConfiguration();
        yml.loadFromString(String.join("\n",
                "cost: 5",
                "arguments:",
                "  buy:",
                "    cost: 100.5",
                "  free:",
                "    type: LITERAL"));

        Node root = new Node("shop", Node.Type.LITERAL, yml, warnings);
        assertEquals(5, root.getCost());
        assertEquals(100.5, root.find(null, "buy").getCost());
        assertEquals(-1, root.find(null, "free").getCost());
    }

    @Test
    void diceShowsFacesAndTotal() throws Exception {
        Node root = load("commands/dice.yml");
        assertTrue(warnings.isEmpty(), warnings::toString);
        assertEquals("1", root.getValues().get("amount"));

        Node three = root.find(null, "3");
        assertEquals(root.getMessages(), three.getMessages(), "the roll anchor is shared");

        Map<String, String> values = new LinkedHashMap<>(root.getValues());
        values.put("amount", "3");
        rollAll(root, values, new Random(11));

        String[] faces = values.get("roll").split(" ");
        assertEquals(3, faces.length);
        assertEquals("3", values.get("roll.count"));

        int sum = 0;
        for (int i = 1; i <= 3; i++) {
            int value = Integer.parseInt(values.get("roll." + i + ".value"));
            assertEquals(String.valueOf((char) ('\u2680' + value - 1)), values.get("roll." + i));
            sum += value;
        }
        assertEquals(String.valueOf(sum), values.get("roll.sum"));

        String line = Lines.parse(three.getMessages().get(0), 1000).getText();
        assertEquals("&7You rolled &f" + values.get("roll") + " &7(&e" + sum + "&7)", Lines.apply(line, values));
    }

    @Test
    void drawsRollTimesAndStopWhenUniqueRunsOut() throws Exception {
        YamlConfiguration yml = new YamlConfiguration();
        yml.loadFromString(String.join("\n",
                "random-values:",
                "  cards:",
                "    times: '{amount}'",
                "    unique: true",
                "    separator: ' | '",
                "    entries: [ '[value:10]A', '[value:20]B' ]",
                "  word: 'yes|no'"));

        Node root = new Node("test", Node.Type.LITERAL, yml, warnings);
        assertTrue(warnings.isEmpty(), warnings::toString);

        Map<String, String> values = new LinkedHashMap<>();
        values.put("amount", "5");
        rollAll(root, values, new Random(2));

        assertEquals("2", values.get("cards.count"));
        assertEquals("30", values.get("cards.sum"));
        assertTrue(values.get("cards").equals("A | B") || values.get("cards").equals("B | A"), values.get("cards"));
        assertTrue(values.get("cards.value").contains("10"));
        assertEquals("1", values.get("word.count"));
        assertEquals("", values.get("word.sum"));
    }

    @Test
    void onceValuesAreSolvedASingleTime() throws Exception {
        YamlConfiguration yml = new YamlConfiguration();
        yml.loadFromString(String.join("\n",
                "values:",
                "  claims: '[once]{calc:{saved-player:claims}+1}'",
                "  double: '[ONCE]{calc:{dice.sum}*2}'",
                "  live: '{saved-player:claims}'",
                "random-values:",
                "  dice: { times: 2, entries: [ '[value:3]x' ] }"));

        Node root = new Node("test", Node.Type.LITERAL, yml, warnings);
        assertTrue(warnings.isEmpty(), warnings::toString);
        assertEquals(new HashSet<>(Arrays.asList("claims", "double")), root.getOnce());

        int[] saved = {4};
        Map<String, String> values = new LinkedHashMap<>();
        Node.fill(Collections.singletonList(root), values, new Random(), 1000,
                (name, spec) -> name.equals("saved-player") ? String.valueOf(saved[0]) : null);

        assertEquals("5", values.get("claims"));
        assertEquals("12", values.get("double"), "it is solved after the random values");
        assertEquals("{saved-player:claims}", values.get("live"), "other values are solved in each line");

        saved[0] = 5;
        assertEquals("5", Lines.evaluate(Lines.apply("{claims}", values), new Random(), (name, spec) -> "x"));
    }

    @Test
    void typedValuesWinOverOnceValues() throws Exception {
        YamlConfiguration yml = new YamlConfiguration();
        yml.loadFromString("values: { amount: '[once]{calc:1+1}' }");

        Map<String, String> values = new LinkedHashMap<>();
        values.put("amount", "7");
        Node.fill(Collections.singletonList(new Node("test", Node.Type.LITERAL, yml, warnings)), values,
                new Random(), 1000, (name, spec) -> null);
        assertEquals("7", values.get("amount"));
    }

    @Test
    void requiresConfirmAndMessagesAreRead() throws Exception {
        YamlConfiguration yml = new YamlConfiguration();
        yml.loadFromString(String.join("\n",
                "requires: [ '{level}>=5' ]",
                "deny-message: 'Level 5 first.'",
                "arguments:",
                "  wipe:",
                "    confirm: 10",
                "    confirm-message: 'Sure?'"));

        Node root = new Node("test", Node.Type.LITERAL, yml, warnings);
        assertEquals(Collections.singletonList("{level}>=5"), root.getRequires());
        assertEquals("Level 5 first.", root.getDenyMessage());
        assertEquals(0, root.getConfirm());

        Node wipe = root.find(null, "wipe");
        assertEquals(10, wipe.getConfirm());
        assertEquals("Sure?", wipe.getConfirmMessage());
    }

    @Test
    void argumentPermissionsOnlyFillTheFirstLevel() throws Exception {
        YamlConfiguration yml = new YamlConfiguration();
        yml.loadFromString(String.join("\n",
                "arguments:",
                "  chat:",
                "    arguments:",
                "      centered: { type: LITERAL }",
                "  title:",
                "    permission: custom.title",
                "  amount:",
                "    type: INTEGER"));

        Node root = new Node("print", Node.Type.LITERAL, yml, warnings);
        root.derivePermissions("sir.print");

        assertEquals("sir.print.chat", root.find(null, "chat").getPermission());
        assertEquals("", root.find(null, "chat").find(null, "centered").getPermission());
        assertEquals("custom.title", root.find(null, "title").getPermission());
        assertEquals("", root.find(null, "5").getPermission());
    }

    @Test
    void materialAndEntityArgumentsGiveTheirNames() throws Exception {
        YamlConfiguration yml = new YamlConfiguration();
        yml.loadFromString(String.join("\n",
                "arguments:",
                "  item:",
                "    type: MATERIAL",
                "    arguments:",
                "      mob: { type: ENTITY }"));

        Node item = new Node("test", Node.Type.LITERAL, yml, warnings).getChildren().get(0);
        assertTrue(item.matches(null, "DIAMOND"));
        assertTrue(item.matches(null, "minecraft:oak_log"));
        assertFalse(item.matches(null, "diamnd"));
        assertEquals("oak_log", item.value(null, "minecraft:oak_log"));
        assertTrue(item.suggest(null).contains("diamond"));

        Node mob = item.getChildren().get(0);
        assertTrue(mob.matches(null, "Zombie"));
        assertTrue(mob.matches(null, "minecraft:cow"));
        assertFalse(mob.matches(null, "dragonfly"));
        assertEquals("zombie", mob.value(null, "ZOMBIE"));
    }

    @Test
    void printUsesArgumentPermissions() throws Exception {
        YamlConfiguration yml;
        try (java.io.Reader reader = new java.io.InputStreamReader(
                getClass().getClassLoader().getResourceAsStream("commands/print.yml"),
                java.nio.charset.StandardCharsets.UTF_8)) {
            yml = YamlConfiguration.loadConfiguration(reader);
        }

        assertTrue(yml.getBoolean("argument-permissions"));
        Node root = new Node("print", Node.Type.LITERAL, yml, warnings);
        root.derivePermissions(yml.getString("permission"));

        assertEquals("sir.print.toast", root.find(null, "toast").getPermission());
        assertEquals("sir.print.action-bar", root.find(null, "action_bar").getPermission());
        assertEquals("sir.print.channel", root.find(null, "bossbar:10:RED").getPermission());
    }

    @Test
    void dailyAndCoinFilesWork() throws Exception {
        Node daily = load("commands/daily.yml");
        assertTrue(warnings.isEmpty(), warnings::toString);

        Map<String, String> values = rollAll(daily, new LinkedHashMap<>(), new Random(4));
        assertEquals("3", values.get("reward.count"));
        assertNotEquals(values.get("reward.1"), values.get("reward.2"));
        assertEquals("[for:reward]give {player} {reward.value}", daily.getCommands().get(0));
        assertEquals("reward", Lines.parse(daily.getCommands().get(0), 1000).getEach());

        Node coin = load("commands/coin.yml");
        String line = Lines.parse(coin.getMessages().get(0), 1000).getText();
        Map<String, String> side = new LinkedHashMap<>();

        side.put("side", "edge");
        assertEquals("&7You flipped a coin and it landed on its &eedge&7! :)", Lines.evaluate(Lines.apply(line, side), new Random()));
        side.put("side", "heads");
        assertEquals("&7You flipped a coin and got &eheads&7.", Lines.evaluate(Lines.apply(line, side), new Random()));
    }

    @Test
    void printSharesOneMessageArgumentForEveryChannel() throws Exception {
        Node root = load("commands/print.yml");
        assertTrue(warnings.isEmpty(), warnings::toString);

        Node bar = root.find(null, "action-bar"), tab = root.find(null, "tab_header"), other = root.find(null, "bossbar:10:RED");
        assertEquals("[action_bar]", bar.getValues().get("prefix"));
        assertEquals("tab-header", tab.getName());
        assertEquals("[tab_header]", tab.getValues().get("prefix"));
        assertEquals("channel", other.getName());
        assertEquals("[{channel}]", other.getValues().get("prefix"));

        for (String channel : new String[] {"chat", "bossbar", "tab-footer", "toast", "json"})
            assertNotNull(root.find(null, channel).find(null, "@a").find(null, "hi"), channel);
        assertEquals(bar.find(null, "@a").find(null, "hi").getMessages(),
                other.find(null, "@a").find(null, "hi").getMessages());
        assertEquals("[title:{seconds}]", root.find(null, "title").find(null, "@a").find(null, "5").getValues().get("prefix"));
    }

    @Test
    void outcomesFollowWeightsPermissionsAndUniqueness() throws Exception {
        YamlConfiguration yml = new YamlConfiguration();
        yml.loadFromString(String.join("\n",
                "random:",
                "  picks: 2",
                "  outcomes:",
                "    common: { weight: 9 }",
                "    rare: { weight: 1 }",
                "    never: { weight: 0 }",
                "    locked: { permission: secret }"));

        Node node = new Node("test", Node.Type.LITERAL, yml, warnings);
        assertTrue(warnings.stream().anyMatch(w -> w.contains("'never'")));

        java.util.Random random = new java.util.Random(7);
        java.util.Map<String, Integer> counts = new java.util.HashMap<>();

        for (int i = 0; i < 2000; i++) {
            List<Node.Outcome> picked = node.pick(perm -> false, random);
            assertEquals(2, picked.size());
            assertNotEquals(picked.get(0).getName(), picked.get(1).getName(), "unique picks");
            picked.forEach(o -> counts.merge(o.getName(), 1, Integer::sum));
        }

        assertFalse(counts.containsKey("never"));
        assertFalse(counts.containsKey("locked"));
        assertEquals(2000, (int) counts.get("common"));
        assertEquals(2000, (int) counts.get("rare"), "only two outcomes are left, so both are always picked");

        boolean locked = false;
        for (int i = 0; i < 200 && !locked; i++)
            locked = node.pick(perm -> true, random).stream().anyMatch(o -> o.getName().equals("locked"));
        assertTrue(locked, "permitted senders can get the locked outcome");
    }

    @Test
    void weightsSetTheOdds() throws Exception {
        YamlConfiguration yml = new YamlConfiguration();
        yml.loadFromString("random:\n  outcomes:\n    common: { weight: 9 }\n    rare: { weight: 1 }");

        Node node = new Node("test", Node.Type.LITERAL, yml, warnings);
        java.util.Random random = new java.util.Random(3);

        int rare = 0;
        for (int i = 0; i < 10000; i++)
            if (node.pick(perm -> true, random).get(0).getName().equals("rare")) rare++;

        assertTrue(rare > 800 && rare < 1200, "about 10%: " + rare);
    }

    @Test
    void invalidTypesAreRejected() {
        YamlConfiguration yml = new YamlConfiguration();
        yml.set("arguments.bad.type", "WORD");

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> new Node("test", Node.Type.LITERAL, yml, warnings));
        assertTrue(e.getMessage().contains("'WORD'"));
    }
}
