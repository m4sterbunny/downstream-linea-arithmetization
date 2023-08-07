/*
 * Copyright ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package net.consensys.linea.zktracer.opcode.stack;

import net.consensys.linea.zktracer.opcode.gas.Gas;

// TODO: maybe a builder?
public record StackSettings(
    Pattern pattern,
    int alpha,
    int delta,
    int nbAdded,
    int nbRemoved,
    Gas staticGas,
    boolean twoLinesInstruction,
    boolean staticInstruction,
    boolean addressTrimmingInstruction,
    boolean oobFlag,
    boolean flag1,
    boolean flag2,
    boolean flag3,
    boolean flag4) {}
