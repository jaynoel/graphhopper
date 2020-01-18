/*
 *  Licensed to GraphHopper GmbH under one or more contributor
 *  license agreements. See the NOTICE file distributed with this work for
 *  additional information regarding copyright ownership.
 *
 *  GraphHopper GmbH licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except in
 *  compliance with the License. You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.graphhopper.routing.weighting;

import com.graphhopper.routing.profiles.*;
import com.graphhopper.routing.util.CarFlagEncoder;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.routing.util.FlexModel;
import com.graphhopper.storage.GraphBuilder;
import com.graphhopper.storage.GraphHopperStorage;
import com.graphhopper.util.EdgeIteratorState;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static com.graphhopper.routing.profiles.RoadClass.*;
import static org.junit.Assert.assertEquals;

public class FlexModelWeightingTest {

    GraphHopperStorage graphHopperStorage;
    DecimalEncodedValue avSpeedEnc;
    BooleanEncodedValue accessEnc;
    EnumEncodedValue<RoadClass> roadClassEnc;
    EncodingManager encodingManager;
    FlagEncoder carFE;

    @Before
    public void setUp() {
        carFE = new CarFlagEncoder();
        encodingManager = new EncodingManager.Builder().add(carFE).build();
        avSpeedEnc = carFE.getAverageSpeedEnc();
        accessEnc = carFE.getAccessEnc();
        roadClassEnc = encodingManager.getEnumEncodedValue(KEY, RoadClass.class);
        graphHopperStorage = new GraphBuilder(encodingManager).create();
    }

    @Test
    public void testBasic() {
        EdgeIteratorState edge1 = graphHopperStorage.edge(0, 1).setDistance(10).set(roadClassEnc, PRIMARY).set(avSpeedEnc, 80).set(accessEnc, true).setReverse(accessEnc, true);
        EdgeIteratorState edge2 = graphHopperStorage.edge(1, 2).setDistance(10).set(roadClassEnc, SECONDARY).set(avSpeedEnc, 70).set(accessEnc, true).setReverse(accessEnc, true);
        graphHopperStorage.edge(2, 3).setDistance(10).set(roadClassEnc, SECONDARY).set(avSpeedEnc, 70).set(accessEnc, true).setReverse(accessEnc, true);
        graphHopperStorage.edge(3, 4).setDistance(10).set(roadClassEnc, MOTORWAY).set(avSpeedEnc, 100).set(accessEnc, true).setReverse(accessEnc, true);

        FlexModel vehicleModel = new FlexModel();
        vehicleModel.setMaxSpeed(120);
        vehicleModel.setBase("car");
        Map map = new HashMap();
        map.put(PRIMARY.toString(), 2.0);
        vehicleModel.getPriority().put(KEY, map);

        Weighting weighting = new FlexModelWeighting("flex", vehicleModel, carFE, encodingManager, new DefaultEncodedValueFactory());
        assertEquals(10.5, weighting.calcEdgeWeight(edge2, false), 0.1);
        assertEquals(5.2, weighting.calcEdgeWeight(edge1, false), 0.1);

        map.put(PRIMARY.toString(), 1.1);
        weighting = new FlexModelWeighting("flex", vehicleModel, carFE, encodingManager, new DefaultEncodedValueFactory());
        assertEquals(9.5, weighting.calcEdgeWeight(edge1, false), 0.11);

        // force integer value
        map.put(PRIMARY.toString(), 1);
        weighting = new FlexModelWeighting("flex", vehicleModel, carFE, encodingManager, new DefaultEncodedValueFactory());
        assertEquals(10.5, weighting.calcEdgeWeight(edge1, false), 0.11);
    }


    @Test
    public void testNoMaxSpeed() {
        EdgeIteratorState edge1 = graphHopperStorage.edge(0, 1).setDistance(10).set(roadClassEnc, PRIMARY).set(avSpeedEnc, 80).set(accessEnc, true).setReverse(accessEnc, true);
        FlexModel vehicleModel = new FlexModel();
        vehicleModel.setBase("car");

        Weighting weighting = new FlexModelWeighting("flex", vehicleModel, carFE, encodingManager, new DefaultEncodedValueFactory());
        assertEquals(10.5, weighting.calcEdgeWeight(edge1, false), 0.1);
    }
}