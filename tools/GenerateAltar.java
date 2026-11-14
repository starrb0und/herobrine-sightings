import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

/**
 * Writes the structure template for Herobrine's altar. Edit the layout below, then run from the
 * project root:
 *
 * <pre>java tools/GenerateAltar.java</pre>
 */
public class GenerateAltar {
	private static final Path OUTPUT = Path.of("src/main/resources/data/herobrine/structure/altar.nbt");
	/** Minecraft 26.2's world version, from version.json in the game jar. */
	private static final int DATA_VERSION = 4903;

	private static final List<String> palette = new ArrayList<>();
	private static final Map<String, Map<String, String>> properties = new LinkedHashMap<>();
	private static final List<int[]> blocks = new ArrayList<>();

	public static void main(String[] args) throws IOException {
		properties.put("minecraft:redstone_torch", Map.of("lit", "true"));

		// Layer 0: a 5x5 cobblestone footing, set flush with the ground.
		for (int x = 0; x < 5; x++) {
			for (int z = 0; z < 5; z++) {
				set(x, 0, z, (x * 3 + z * 5) % 4 == 0 ? "minecraft:cobblestone" : "minecraft:mossy_cobblestone");
			}
		}
		// Layer 1: a 3x3 ring of gold around the fire block.
		for (int x = 1; x <= 3; x++) {
			for (int z = 1; z <= 3; z++) {
				set(x, 1, z, "minecraft:gold_block");
			}
		}
		// Soul fire rather than normal fire, so the altar can never set its surroundings alight.
		set(2, 1, 2, "minecraft:soul_soil");
		set(2, 2, 2, "minecraft:soul_fire");
		// Layer 2: a redstone torch on the middle of each side.
		set(1, 2, 2, "minecraft:redstone_torch");
		set(3, 2, 2, "minecraft:redstone_torch");
		set(2, 2, 1, "minecraft:redstone_torch");
		set(2, 2, 3, "minecraft:redstone_torch");

		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (DataOutputStream out = new DataOutputStream(new GZIPOutputStream(bytes))) {
			writeTemplate(out);
		}
		Files.createDirectories(OUTPUT.getParent());
		Files.write(OUTPUT, bytes.toByteArray());
		System.out.println("Wrote " + OUTPUT + " (" + blocks.size() + " blocks)");
	}

	private static void set(int x, int y, int z, String block) {
		if (!palette.contains(block)) {
			palette.add(block);
		}
		blocks.removeIf(b -> b[0] == x && b[1] == y && b[2] == z);
		blocks.add(new int[] {x, y, z, palette.indexOf(block)});
	}

	// --- Minimal NBT writer: just the tag types a structure template needs. ---

	private static final int END = 0, INT = 3, STRING = 8, LIST = 9, COMPOUND = 10;

	private static void writeTemplate(DataOutputStream out) throws IOException {
		header(out, COMPOUND, "");
		intTag(out, "DataVersion", DATA_VERSION);
		intList(out, "size", 5, 3, 5);

		listHeader(out, "palette", COMPOUND, palette.size());
		for (String block : palette) {
			stringTag(out, "Name", block);
			Map<String, String> props = properties.get(block);
			if (props != null) {
				header(out, COMPOUND, "Properties");
				for (Map.Entry<String, String> prop : props.entrySet()) {
					stringTag(out, prop.getKey(), prop.getValue());
				}
				out.writeByte(END);
			}
			out.writeByte(END);
		}

		listHeader(out, "blocks", COMPOUND, blocks.size());
		for (int[] block : blocks) {
			intList(out, "pos", block[0], block[1], block[2]);
			intTag(out, "state", block[3]);
			out.writeByte(END);
		}

		listHeader(out, "entities", END, 0);
		out.writeByte(END);
	}

	private static void header(DataOutputStream out, int type, String name) throws IOException {
		out.writeByte(type);
		out.writeUTF(name);
	}

	private static void intTag(DataOutputStream out, String name, int value) throws IOException {
		header(out, INT, name);
		out.writeInt(value);
	}

	private static void stringTag(DataOutputStream out, String name, String value) throws IOException {
		header(out, STRING, name);
		out.writeUTF(value);
	}

	private static void listHeader(DataOutputStream out, String name, int elementType, int length) throws IOException {
		header(out, LIST, name);
		out.writeByte(elementType);
		out.writeInt(length);
	}

	private static void intList(DataOutputStream out, String name, int... values) throws IOException {
		listHeader(out, name, INT, values.length);
		for (int value : values) {
			out.writeInt(value);
		}
	}
}
