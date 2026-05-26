/*
 * This file is part of ViaBedrock - https://github.com/RaphiMC/ViaBedrock
 * Copyright (C) 2023-2026 RK_01/RaphiMC and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.raphimc.viabedrock.protocol.rewriter.resourcepack;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.viaversion.viaversion.api.minecraft.item.data.ItemModel;
import com.viaversion.viaversion.util.Key;
import net.raphimc.viabedrock.api.resourcepack.ResourcePack;
import net.raphimc.viabedrock.api.resourcepack.content.Content;
import net.raphimc.viabedrock.api.resourcepack.definition.AttachableDefinitions;
import net.raphimc.viabedrock.api.util.StringUtil;
import net.raphimc.viabedrock.protocol.storage.ResourcePackStorage;
import org.cube.converter.converter.enums.RotationType;
import org.cube.converter.model.impl.bedrock.BedrockGeometryModel;
import org.cube.converter.model.impl.java.JavaItemModel;

import java.util.HashMap;
import java.util.Map;

public class CustomAttachableResourceRewriter extends ItemModelResourceRewriter {

    private static final String SUB_FOLDER = "attachables";

    public static ItemModel getItemModel(final String attachableIdentifier) {
        return new ItemModel(Key.of("viabedrock", SUB_FOLDER + '/' + StringUtil.makeIdentifierValueSafe(attachableIdentifier)));
    }

    public CustomAttachableResourceRewriter() {
        super(SUB_FOLDER);
    }

    @Override
    public void apply(final ResourcePackStorage resourcePackStorage, final Content javaContent) {
        for (Map.Entry<String, AttachableDefinitions.AttachableDefinition> attachableEntry : resourcePackStorage.getAttachables().attachables().entrySet()) {
            final AttachableDefinitions.AttachableDefinition attachableDefinition = attachableEntry.getValue();
            final Map<String, String> javaModelDefinitions = new HashMap<>();
            for (String bedrockPath : attachableDefinition.attachableData().getTextures().values()) {
                for (ResourcePack pack : resourcePackStorage.getPackStackTopToBottom()) {
                    final Content.LazyImage texture = pack.content().getShortnameImage(bedrockPath);
                    if (texture != null) {
                        javaContent.putPngImage("assets/viabedrock/textures/" + this.getJavaTexturePath(bedrockPath) + ".png", texture);
                        break;
                    }
                }
            }
            for (Map.Entry<String, String> modelEntry : attachableDefinition.attachableData().getGeometries().entrySet()) {
                final BedrockGeometryModel bedrockGeometry = resourcePackStorage.getModels().entityModels().get(modelEntry.getValue());
                if (bedrockGeometry == null) {
                    continue;
                }
                if (!attachableDefinition.attachableData().getTextures().containsKey(modelEntry.getKey())) {
                    continue;
                }

                final String javaTexturePath = this.getJavaTexturePath(attachableDefinition.attachableData().getTextures().get(modelEntry.getKey()));
                final JavaItemModel itemModelData = bedrockGeometry.toJavaItemModel("viabedrock:" + javaTexturePath, RotationType.POST_1_21_11);
                final JSONObject itemModel = itemModelData.compile();

                final JSONObject display = new JSONObject();
                final JSONArray scaling = new JSONArray();
                scaling.add(itemModelData.getScale());
                scaling.add(itemModelData.getScale());
                scaling.add(itemModelData.getScale());

                final JSONObject value = new JSONObject();
                value.put("scale", scaling);

                display.put("firstperson_righthand", value);
                display.put("firstperson_lefthand", value);
                display.put("thirdperson_righthand", value);
                display.put("thirdperson_lefthand", value);
                display.put("head", value);
                display.put("gui", value);
                display.put("ground", value);
                display.put("fixed", value);

                itemModel.put("display", display);

                final String modelKey = attachableEntry.getKey() + "_" + modelEntry.getKey();
                javaModelDefinitions.put(modelKey, itemModel.toString());
                resourcePackStorage.getConverterData().put("ca_" + attachableEntry.getKey() + '_' + modelKey, true);
            }
            this.putItemDefinition(javaContent, attachableEntry.getKey(), javaModelDefinitions);
        }
    }

}
