/*
 * This file is part of Movecraft.
 *
 *     Movecraft is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Movecraft is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Movecraft.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.countercraft.movecraft.async.translation;

import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.async.AsyncTask;
import net.countercraft.movecraft.bungee.PingUtils;
import net.countercraft.movecraft.bungee.RepeatTryServerJumpTask;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.craft.CraftType;
import net.countercraft.movecraft.event.CraftAsyncTranslateEvent;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.utils.AccelerationUtils;
import net.countercraft.movecraft.utils.BoundingBoxUtils;
import net.countercraft.movecraft.utils.CarUtils;
import net.countercraft.movecraft.utils.EntityUpdateCommand;
import net.countercraft.movecraft.utils.LocationUtils;
import net.countercraft.movecraft.utils.MapUpdateCommand;
import net.countercraft.movecraft.utils.MapUpdateManager;
import net.countercraft.movecraft.utils.MathUtils;
import net.countercraft.movecraft.utils.MovecraftLocation;

import org.apache.commons.collections.ListUtils;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

public class TranslationTask extends AsyncTask {
        private TranslationTaskData data;
        public TranslationTask( Craft c, TranslationTaskData data ) {
                super( c );
                this.data = data;
        }

        @SuppressWarnings("deprecation")
		@Override
        public void excecute() {
        	
        		if(getCraft().getType().isGroundVehicle()){
        			data.setDy(CarUtils.getNewdY(getCraft(), data.getDx(), data.getDy()));
        		}
        		
                MovecraftLocation[] blocksList = data.getBlockList();
                
                // canfly=false means an ocean-going vessel
                boolean waterCraft=!getCraft().getType().canFly();
                int waterLine=0;
                if (waterCraft) {
                        int [][][] hb=getCraft().getHitBox();
                        
                        // start by finding the minimum and maximum y coord
                        int minY=65535;
                        int maxY=-65535;
                        for (int [][] i1 : hb) {
                                for (int [] i2 : i1) {
                                        if(i2!=null) {
                                                if(i2[0]<minY) {
                                                        minY=i2[0];
                                                }
                                                if(i2[1]>maxY) {
                                                        maxY=i2[1];
                                                }
                                        }
                                }
                        }
                        int maxX=getCraft().getMinX()+hb.length;
                        int maxZ=getCraft().getMinZ()+hb[0].length;  // safe because if the first x array doesn't have a z array, then it wouldn't be the first x array
                        int minX=getCraft().getMinX();
                        int minZ=getCraft().getMinZ();
                        
                        // next figure out the water level by examining blocks next to the outer boundaries of the craft
                        for(int posY=maxY+1; (posY>=minY-1)&&(waterLine==0); posY--) {
                                int posX;
                                int posZ;
                                posZ=minZ-1;
                                for(posX=minX-1; (posX <= maxX+1)&&(waterLine==0); posX++ ) {
                                        if(getCraft().getW().getBlockAt(posX, posY, posZ).getTypeId()==9) {
                                                waterLine=posY;
                                        }
                                }
                                posZ=maxZ+1;
                                for(posX=minX-1; (posX <= maxX+1)&&(waterLine==0); posX++ ) {
                                        if(getCraft().getW().getBlockAt(posX, posY, posZ).getTypeId()==9) {
                                                waterLine=posY;
                                        }
                                }
                                posX=minX-1;
                                for(posZ=minZ; (posZ <= maxZ)&&(waterLine==0); posZ++ ) {
                                        if(getCraft().getW().getBlockAt(posX, posY, posZ).getTypeId()==9) {
                                                waterLine=posY;
                                        }
                                }
                                posX=maxX+1;
                                for(posZ=minZ; (posZ <= maxZ)&&(waterLine==0); posZ++ ) {
                                        if(getCraft().getW().getBlockAt(posX, posY, posZ).getTypeId()==9) {
                                                waterLine=posY;
                                        }
                                }
                        }
                        
                        // now add all the air blocks found within the craft's hitbox immediately above the waterline and below to the craft blocks so they will be translated
                        HashSet<MovecraftLocation> newHSBlockList=new HashSet<MovecraftLocation>(Arrays.asList(blocksList));
                        int posY=waterLine+1;
                        for(int posX=minX; posX<maxX; posX++) {
                                for(int posZ=minZ; posZ<maxZ; posZ++) {
                                        if(hb[posX-minX]!=null) {
                                                if(hb[posX-minX][posZ-minZ]!=null) {
                                                        if(getCraft().getW().getBlockAt(posX,posY,posZ).getTypeId()==0 && posY>hb[posX-minX][posZ-minZ][0] && posY<hb[posX-minX][posZ-minZ][1]) {
                                                                MovecraftLocation l=new MovecraftLocation(posX,posY,posZ);
                                                                newHSBlockList.add(l);
                                                        }
                                                }
                                        }
                                }
                        }
                        // dont check the hitbox for the underwater portion. Otherwise open-hulled ships would flood.
                        for(posY=waterLine; posY>=minY; posY--) {
                                for(int posX=minX; posX<maxX; posX++) {
                                        for(int posZ=minZ; posZ<maxZ; posZ++) {
                                                if(getCraft().getW().getBlockAt(posX,posY,posZ).getTypeId()==0) {
                                                        MovecraftLocation l=new MovecraftLocation(posX,posY,posZ);
                                                        newHSBlockList.add(l);
                                                }
                                        }
                                }
                        }
                                
                        blocksList=newHSBlockList.toArray(new MovecraftLocation[newHSBlockList.size()]);
                } else if(getCraft().getType().isGroundVehicle()){
                	data.setDy(CarUtils.getNewdY(getCraft(), data.getDx(), data.getDz()));
                }
                
                MovecraftLocation[] newBlockList = new MovecraftLocation[blocksList.length];
                HashSet<MovecraftLocation> existingBlockSet = new HashSet<MovecraftLocation>( Arrays.asList( blocksList ) );
                HashSet<EntityUpdateCommand> entityUpdateSet = new HashSet<EntityUpdateCommand>();
                Set<MapUpdateCommand> updateSet = new HashSet<MapUpdateCommand>();
                
                CraftAsyncTranslateEvent event = new CraftAsyncTranslateEvent(getCraft(), data);
                if(!event.call()){
                	fail(event.getMessage());
                }
                for ( int i = 0; i < blocksList.length; i++ ) {
                        MovecraftLocation oldLoc = blocksList[i];
                        MovecraftLocation newLoc = oldLoc.translate( data.getDx(), data.getDy(), data.getDz() );
                        newBlockList[i] = newLoc;

                        if ( newLoc.getY() >= data.getMaxHeight() && newLoc.getY() > oldLoc.getY() ) {
                                fail( String.format( I18nSupport.getInternationalisedString( "Translation - Failed Craft hit height limit" ) ) );
                                break;
                        } else if ( newLoc.getY() <= data.getMinHeight()  && newLoc.getY() < oldLoc.getY() ) {
                                fail( String.format( I18nSupport.getInternationalisedString( "Translation - Failed Craft hit minimum height limit" ) ) );
                                break;
                        }

                        int testID = getCraft().getW().getBlockTypeIdAt( newLoc.getX(), newLoc.getY(), newLoc.getZ() );

                        if(!waterCraft) {
                        	 	int oldID = getCraft().getW().getBlockTypeIdAt( oldLoc.getX(), oldLoc.getY(), oldLoc.getZ() );
                                if ( testID != 0 && !existingBlockSet.contains( newLoc ) ) {
                                        // New block is not air and is not part of the existing ship
                                		
                                        fail( "Craft is obstructed! Blocked by " + getCraft().getW().getBlockAt(newLoc.getX(), newLoc.getY(), newLoc.getZ()).getType().toString().toLowerCase() + " at coordinates " + newLoc.getX() + " , " + newLoc.getY() + " , " + newLoc.getZ());
                                        break;
                                }
                                
                                updateSet.add( new MapUpdateCommand( blocksList[i], newBlockList[i], oldID, getCraft() ) );
                                
                        } else {
                        // let watercraft move through water
                                if ( (testID != 0 && testID != 9 && testID != 8) && !existingBlockSet.contains( newLoc ) ) {
                                        // New block is not air or water and is not part of the existing ship
                                	fail( "Craft is obstructed! Blocked by " + getCraft().getW().getBlockAt(newLoc.getX(), newLoc.getY(), newLoc.getZ()).getType().toString().toLowerCase() + " at coordinates " + newLoc.getX() + " , " + newLoc.getY() + " , " + newLoc.getZ());
                                        break;
                                } else {
                                        int oldID = getCraft().getW().getBlockTypeIdAt( oldLoc.getX(), oldLoc.getY(), oldLoc.getZ() );

                                        updateSet.add( new MapUpdateCommand( blocksList[i], newBlockList[i], oldID, getCraft()) );
                                }
                        }

                }

                if ( !data.failed() ) {
                        data.setBlockList( newBlockList );

                        // Move entities within the craft
                       

                        Iterator<Player> i= getCraft().playersRiding.iterator();
                        while (i.hasNext()) {
                                Entity pTest=i.next();
                                if ( MathUtils.playerIsWithinBoundingPolygon( getCraft().getHitBox(), getCraft().getMinX(), getCraft().getMinZ(), MathUtils.bukkit2MovecraftLoc( pTest.getLocation() ) ) ) {
                                        if(pTest.getType()!=org.bukkit.entity.EntityType.DROPPED_ITEM ) {
                                                Location tempLoc = pTest.getLocation().add( data.getDx(), data.getDy(), data.getDz() );
                                                Location newPLoc=new Location(getCraft().getW(), tempLoc.getX(), tempLoc.getY(), tempLoc.getZ());
                                                newPLoc.setPitch(pTest.getLocation().getPitch());
                                                newPLoc.setYaw(pTest.getLocation().getYaw());
                                                                                           
                                                EntityUpdateCommand eUp=new EntityUpdateCommand(pTest.getLocation(),newPLoc,pTest, pTest.getVelocity(), getCraft());
                                                entityUpdateSet.add(eUp);
                                                pTest.teleport(newPLoc);
                                        } else {
                                                pTest.remove();
                                        }
                                }
                        }
                        
                        getCraft().originalPilotLoc = getCraft().originalPilotLoc.add( data.getDx(), data.getDy(), data.getDz() );

                        
                        //Set blocks that are no longer craft to air
                        List<MovecraftLocation> airLocation = ListUtils.subtract( Arrays.asList( blocksList ), Arrays.asList( newBlockList ) );

                        for ( MovecraftLocation l1 : airLocation ) {
                                // for watercraft, fill blocks below the waterline with water
                                if(!waterCraft) {
                                        updateSet.add( new MapUpdateCommand( l1, 0, getCraft() ) );
                                } else {
                                        if(l1.getY()<=waterLine) {
                                                // if there is air below the ship at the current position, don't fill in with water
                                                MovecraftLocation testAir=new MovecraftLocation(l1.getX(), l1.getY()-1, l1.getZ());
                                                while(existingBlockSet.contains(testAir)) {
                                                        testAir.setY(testAir.getY()-1);
                                                }
                                                if(getCraft().getW().getBlockAt(testAir.getX(), testAir.getY(), testAir.getZ()).getTypeId()==0) {
                                                        updateSet.add( new MapUpdateCommand( l1, 0, getCraft() ) );                                                        
                                                } else {
                                                        updateSet.add( new MapUpdateCommand( l1, 9, getCraft() ) );
                                                }
                                        } else {
                                                updateSet.add( new MapUpdateCommand( l1, 0, getCraft() ) );
                                        }
                                }
                        }

            			MapUpdateCommand[] temp = updateSet.toArray(new MapUpdateCommand[1]);
            			boolean lastUpdateFound = false;
            			for (int i2 = temp.length - 1; i2 >= 0; i2--){
            				MapUpdateCommand temp2 = temp[i2];
            				if(Arrays.binarySearch(MapUpdateManager.getInstance().fragileBlocks,temp2.getTypeID())>=0){
            					temp2.setLastUpdate(true);
            					lastUpdateFound = true;
            					break;
            				}
            			}
            			if (!lastUpdateFound) temp[temp.length - 1].setLastUpdate(true);
            			data.setUpdates( temp );
            			
            			EntityUpdateCommand[] temp2 = entityUpdateSet.toArray( new EntityUpdateCommand[entityUpdateSet.size()] );
                        data.setEntityUpdates(temp2);
                        
                        if ( data.getDy() != 0 ) {
                                data.setHitbox( BoundingBoxUtils.translateBoundingBoxVertically( data.getHitbox(), data.getDy() ) );
                        }

                        data.setMinX( data.getMinX() + data.getDx() );
                        data.setMinZ( data.getMinZ() + data.getDz() );
                        
						//if it should be warping, initiate a warp.
                        Craft c = getCraft();
                        Player p = c.pilot;
						String s = LocationUtils.locationCheck(p);
						if(s != null){
							//if(PingUtils.isOnline(s)){
								c.shipAttemptingTeleport = true;
								RepeatTryServerJumpTask task2 = new RepeatTryServerJumpTask(p, c, s, 0, 205, 0);
								task2.runTaskTimer(Movecraft.getInstance(), 0, 1);
							/*} else {
								p.sendMessage("This planet's server is offline at the moment, you cannot enter.");
							}*/
						}
						//if they aren't in space and are above the warp altitude, initiate planet leaving.
						else if (p.getLocation().getY() > 220){
							if (!LocationUtils.spaceCheck(p, false)){
								//if(PingUtils.isOnline(s)){
									p.sendMessage(ChatColor.RED + "[ALERT]" + ChatColor.GOLD + " Leaving the atmosphere!");
									Location l = LocationUtils.getWarpLocation(p.getWorld().getName());
									c.shipAttemptingTeleport = true;
									RepeatTryServerJumpTask task2 = new RepeatTryServerJumpTask(p, c, LocationUtils.getSystem(), l.getBlockX(), l.getBlockY(), l.getBlockZ());
									task2.runTaskTimer(Movecraft.getInstance(), 0, 1);
									return;
								/*} else {
									p.sendMessage("This planet's space server is offline at the moment, you cannot enter.");
								}*/
							}
						}
						
						//if they are near a stargate initialize solar system jump
						RepeatTryServerJumpTask task2 = LocationUtils.checkStargateJump(p, c);
						if(task2 != null){
							c.shipAttemptingTeleport = true;
							task2.runTaskTimer(Movecraft.getInstance(), 0, 1);
						}
                }
        }

        private void fail( String message ) {
                data.setFailed( true );
                data.setFailMessage( message );
        }

        public TranslationTaskData getData() {
                return data;
        }
}