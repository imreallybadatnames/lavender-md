package io.wispforest.lavendermd.feature;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.wispforest.lavendermd.Lexer;
import io.wispforest.lavendermd.MarkdownFeature;
import io.wispforest.lavendermd.Parser;
import io.wispforest.lavendermd.compiler.MarkdownCompiler;
import io.wispforest.lavendermd.compiler.OwoUICompiler;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.core.Sizing;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.NoSuchElementException;

public class EntityFeature implements MarkdownFeature {

    @Override
    public String name() {
        return "entities";
    }

    @Override
    public boolean supportsCompiler(MarkdownCompiler<?> compiler) {
        return compiler instanceof OwoUICompiler;
    }

    @Override
    public void registerTokens(TokenRegistrar registrar) {
        registrar.registerToken((nibbler, tokens) -> {
            if (!nibbler.tryConsume("<entity;")) return false;

            var entityString = nibbler.consumeUntil('>');
            if (entityString == null) return false;

            try {
                NbtCompound nbt = null;

                int nbtIndex = entityString.indexOf('{');
                if (nbtIndex != -1) {

                    nbt = new StringNbtReader(new StringReader(entityString.substring(nbtIndex))).parseCompound();
                    entityString = entityString.substring(0, nbtIndex);
                }

                var entityType = Registries.ENTITY_TYPE.getOptionalValue(Identifier.of(entityString)).orElseThrow();
                tokens.add(new EntityToken(entityString, entityType, nbt));
                return true;
            } catch (CommandSyntaxException | NoSuchElementException e) {
                return false;
            }
        }, '<');
    }

    @Override
    public void registerNodes(NodeRegistrar registrar) {
        registrar.registerNode(
                (parser, entityToken, tokens) -> new EntityNode(entityToken.type, entityToken.nbt),
                (token, tokens) -> token instanceof EntityToken entity ? entity : null
        );
    }

    private static class EntityToken extends Lexer.Token {

        public final EntityType<?> type;
        public final @Nullable NbtCompound nbt;

        public EntityToken(String content, EntityType<?> type, @Nullable NbtCompound nbt) {
            super(content);
            this.type = type;
            this.nbt = nbt;
        }
    }

    private static class EntityNode extends Parser.Node {

        public final EntityType<?> type;
        public final @Nullable NbtCompound nbt;

        public EntityNode(EntityType<?> type, @Nullable NbtCompound nbt) {
            this.type = type;
            this.nbt = nbt;
        }

        @Override
        protected void visitStart(MarkdownCompiler<?> compiler) {
            ((OwoUICompiler) compiler).visitComponent(Components.entity(Sizing.fixed(32), this.type, this.nbt).scaleToFit(true));
        }

        @Override
        protected void visitEnd(MarkdownCompiler<?> compiler) {}
    }
}
