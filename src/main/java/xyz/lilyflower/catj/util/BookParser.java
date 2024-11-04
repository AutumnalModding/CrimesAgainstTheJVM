package xyz.lilyflower.catj.util;

import java.util.List;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.WrittenBookContentComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.WrittenBookItem;
import net.minecraft.text.Text;

public class BookParser {
    public static String parse(ItemStack book) {
        if (book.getItem() instanceof WrittenBookItem) {
            WrittenBookContentComponent content = book.get(DataComponentTypes.WRITTEN_BOOK_CONTENT);
            if (content != null) {
                List<Text> pages = content.getPages(false);

                StringBuilder resolved = new StringBuilder();
                for (Text page : pages) {
                    resolved.append(page.getString());
                }
                return resolved.toString();
            }
        }

        return "";
    }
}
