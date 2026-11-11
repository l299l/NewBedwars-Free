package com.l299l.newbedwars.api.shop;

/**
 * Result of a {@link ShopEditor} change.
 *
 * @param code   what happened
 * @param detail for {@link Code#INVALID}: the field and reason (e.g. {@code material: unknown material FOO});
 *               for {@link Code#IN_USE}: where it is used (comma separated); otherwise {@code null} or the error
 */
public record ShopResult(Code code, String detail) {
    public enum Code {
        OK,
        /** No item or GUI with that name. */
        NOT_FOUND,
        /** A value is not valid; see {@link #detail()}. */
        INVALID,
        /** The item or GUI is still used and can not be deleted. */
        IN_USE,
        /** The arena is not waiting (a game runs). */
        BUSY,
        /** An item or GUI with that name already exists. */
        NAME_TAKEN,
        /** An error; see {@link #detail()} and the console. */
        FAILED
    }

    public boolean isSuccess() {
        return code == Code.OK;
    }
}
