package dlindustries.vigillant.system.module;

import dlindustries.vigillant.system.utils.EncryptedString;

public enum Category {
	Combat(EncryptedString.of("Combat")),
	CrystalPvP(EncryptedString.of("CrystalPvP")),
	Misc(EncryptedString.of("Misc")),
	RENDER(EncryptedString.of("Render")),
	CLIENT(EncryptedString.of("Client"));
	public final CharSequence name;

	Category(CharSequence name) {
		this.name = name;
	}
}
