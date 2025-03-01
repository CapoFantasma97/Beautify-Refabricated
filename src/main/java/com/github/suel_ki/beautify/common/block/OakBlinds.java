package com.github.suel_ki.beautify.common.block;

import java.util.List;

import com.github.suel_ki.beautify.Beautify;
import com.github.suel_ki.beautify.core.init.SoundInit;
import io.github.fabricators_of_create.porting_lib.block.PlayerDestroyBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class OakBlinds extends HorizontalDirectionalBlock implements PlayerDestroyBlock {
	// Voxelshapes; Hidden = Blind not visible
	private static final VoxelShape OPEN_NORTH = Block.box(0, 13, 13, 16, 16, 16);
	private static final VoxelShape OPEN_SOUTH = Block.box(0, 13, 0, 16, 16, 3);
	private static final VoxelShape OPEN_WEST = Block.box(13, 13, 0, 16, 16, 16);
	private static final VoxelShape OPEN_EAST = Block.box(0, 13, 0, 3, 16, 16);
	private static final VoxelShape CLOSED_SOUTH = Block.box(0, 0, 0, 16, 16, 2);
	private static final VoxelShape CLOSED_NORTH = Block.box(0, 0, 13, 16, 16, 16);
	private static final VoxelShape CLOSED_EAST = Block.box(0, 0, 0, 3, 16, 16);
	private static final VoxelShape CLOSED_WEST = Block.box(13, 0, 0, 16, 16, 16);
	private static final VoxelShape SHAPE_HIDDEN = Block.box(0, 0, 0, 0, 0, 0);
	// Open = true if blinds are down; Hidden = true if blinds are closed and there
	// is a blind above
	public static final BooleanProperty OPEN = BooleanProperty.create("open");
	public static final BooleanProperty HIDDEN = BooleanProperty.create("hidden");

	// constructor
	public OakBlinds(Properties properties) {
		super(properties);
		this.registerDefaultState(this.defaultBlockState().setValue(OPEN, false).setValue(FACING, Direction.NORTH)
				.setValue(HIDDEN, false));
	}
	
	@Override
	public boolean isCollisionShapeFullBlock(BlockState state, BlockGetter level, BlockPos pos) {
		return false;
	}
	
	@Override
	public VoxelShape getBlockSupportShape(BlockState state, BlockGetter level, BlockPos pos) {
		return Shapes.empty();
	}
	
	@Override
	public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
		return true;
	}

	// check for facing placement, hidden is false per default
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite())
				.setValue(OPEN, false).setValue(HIDDEN, false);
	}

	// creating blockstates
	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(OPEN, FACING, HIDDEN);
	}

	// INTERACTION
	// changes blockstates:
	// OPEN: open <-> closed
	// HIDDEN: false <-> true if below root
	@Override
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
			BlockHitResult result) {
		if (!level.isClientSide() && hand == InteractionHand.MAIN_HAND && player.getItemInHand(hand).isEmpty()) {
			// stores last value of blind
			final boolean currentlyOpen = state.getValue(OPEN);

			// if the blinds open from the root
			// search for the position of the topmost blind and set the pos
			if (Beautify.CONFIG.blinds.opensFromRoot) {
				int step = 1;
				while (sameBlindType(level, pos.above(step), state)) {
					++step;
				}
				pos = pos.above(step - 1);
			}

			{
				// changes clicked blind: open <-> closed
				level.setBlock(pos, state.setValue(OPEN, !currentlyOpen), 3);

				// CODE BELOW IS DISABLED IF SEARCHRADIUS = 0
				// checks for blinds below clicked blind: open <-> closed, hidden=true
				if (Beautify.CONFIG.blinds.searchRadius > 0) {
					for (int offsetDown = 1; offsetDown <= Beautify.CONFIG.blinds.searchRadius; ++offsetDown) {
						if (sameBlindType(level, pos.below(offsetDown), state)) {
							switchOpenUpdateHidden(level, pos.below(offsetDown), state, false);
						} else {
							break;
						}
					}
				}

				if (Beautify.CONFIG.blinds.searchRadius > 0) {
					// FOR BLINDS ON NORTH-SOUTH AXIS
					if (state.getValue(FACING) == Direction.NORTH || state.getValue(FACING) == Direction.SOUTH) {

						// checks blinds east of clicked blind
						for (int offsetEast = 1; offsetEast <= (int) Beautify.CONFIG.blinds.searchRadius / 2; ++offsetEast) {
							if (sameBlindType(level, pos.east(offsetEast), state)) {
								// changes east blinds: open <-> closed
								level.setBlock(pos.east(offsetEast), state.setValue(OPEN, !currentlyOpen), 3);
								// checks for blinds below east blinds: open <-> closed, hidden=true
								for (int offsetDown = 1; offsetDown <= Beautify.CONFIG.blinds.searchRadius; ++offsetDown) {
									if (sameBlindType(level, pos.below(offsetDown).east(offsetEast), state)) {
										switchOpenUpdateHidden(level, pos.below(offsetDown).east(offsetEast), state,
												false);
									} else {
										break;
									}
								}
							} else {
								break;
							}
						}

						// checks blinds west of clicked blind
						for (int offsetWest = 1; offsetWest <= Beautify.CONFIG.blinds.searchRadius / 2; ++offsetWest) {
							if (sameBlindType(level, pos.west(offsetWest), state)) {
								// changes west blinds: open <-> closed
								level.setBlock(pos.west(offsetWest), state.setValue(OPEN, !currentlyOpen), 3);
								// checks for blinds below west blinds: open <-> closed, hidden=true
								for (int offsetDown = 1; offsetDown <= Beautify.CONFIG.blinds.searchRadius; ++offsetDown) {
									if (sameBlindType(level, pos.below(offsetDown).west(offsetWest), state)) {
										switchOpenUpdateHidden(level, pos.below(offsetDown).west(offsetWest), state,
												false);
									} else {
										break;
									}
								}
							} else {
								break;
							}
						}
					}

					// FOR BLINDS ON EAST-WEST AXIS
					if (state.getValue(FACING) == Direction.EAST || state.getValue(FACING) == Direction.WEST) {

						// checks blinds north of clicked blind
						for (int offsetNorth = 1; offsetNorth <= Beautify.CONFIG.blinds.searchRadius / 2; ++offsetNorth) {
							if (sameBlindType(level, pos.north(offsetNorth), state)) {
								// changes north blinds: open <-> closed
								level.setBlock(pos.north(offsetNorth), state.setValue(OPEN, !currentlyOpen), 3);
								// checks for blinds below north blinds: open <-> closed, hidden=true
								for (int offsetDown = 1; offsetDown <= Beautify.CONFIG.blinds.searchRadius; ++offsetDown) {
									if (sameBlindType(level, pos.below(offsetDown).north(offsetNorth), state)) {
										switchOpenUpdateHidden(level, pos.below(offsetDown).north(offsetNorth),
												state, false);
									} else {
										break;
									}
								}
							} else {
								break;
							}
						}

						// checks blinds south of clicked blind
						for (int offsetSouth = 1; offsetSouth <= Beautify.CONFIG.blinds.searchRadius / 2; ++offsetSouth) {
							if (sameBlindType(level, pos.south(offsetSouth), state)) {
								// changes south blinds: open <-> closed
								level.setBlock(pos.south(offsetSouth), state.setValue(OPEN, !currentlyOpen), 3);
								// checks for blinds below south blinds: open <-> closed, hidden=true
								for (int offsetDown = 1; offsetDown <= Beautify.CONFIG.blinds.searchRadius; ++offsetDown) {
									if (sameBlindType(level, pos.below(offsetDown).south(offsetSouth), state)) {
										switchOpenUpdateHidden(level, pos.below(offsetDown).south(offsetSouth),
												state, false);
									} else {
										break;
									}
								}
							} else {
								break;
							}
						}
					}
					level.playSound(null, pos,
							currentlyOpen ? SoundInit.BLINDS_CLOSE : SoundInit.BLINDS_OPEN,
							SoundSource.BLOCKS, 1, 1);
					return InteractionResult.SUCCESS;
				}
			}
		}
		return InteractionResult.SUCCESS;
	}

	// returns: true/false if
	// block in level at pos is the same kind of blind
	// and facing is the same as state
	private boolean sameBlindType(Level level, BlockPos pos, BlockState state) {
		return level.getBlockState(pos).getBlock().getClass() == this.getClass()
				&& level.getBlockState(pos).getValue(FACING) == state.getValue(FACING);
	}

	// method for changing the blockstates of blinds
	// if updateOnly is true, only HIDDEN is changed
	// if updateOnly is false, the blind will also open or close

	private void switchOpenUpdateHidden(Level level, BlockPos pos, BlockState state, boolean updateOnly) {
		if (updateOnly) {
			level.setBlock(pos, state.setValue(HIDDEN, false), 3);
			return;
		}

		if (!state.getValue(OPEN)) {
			level.setBlock(pos, state.setValue(OPEN, true).setValue(HIDDEN, false), 3);
		} else {
			level.setBlock(pos, state.setValue(OPEN, false).setValue(HIDDEN, true), 3);
		}
	}

	// method to prevent hidden blinds from being unaccessible
	// after root block is destroyed.
	// updates blind below the destroyed block.
	@Override
	public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, boolean willHarvest,
			FluidState fluid) {
		if (sameBlindType(level, pos.below(), state)) {
			switchOpenUpdateHidden(level, pos.below(), state, true);
		}
		return PlayerDestroyBlock.super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
	}

	@Override
	public void wasExploded(Level level, BlockPos pos, Explosion explosion) {
		BlockState state = level.getBlockState(pos);
		if (sameBlindType(level, pos.below(), state)) {
			switchOpenUpdateHidden(level, pos.below(), state, true);
		}
		super.wasExploded(level, pos, explosion);
	}
	// hidden = invisible model
	// OPEN_X = models of blinds that are down
	// CLOSED_X= models of blinds that are up
	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		if (state.getValue(HIDDEN)) {
			return SHAPE_HIDDEN;
		}

		if (!state.getValue(OPEN)) {
			return switch (state.getValue(FACING)) {
			case NORTH -> OPEN_NORTH;
			case SOUTH -> OPEN_SOUTH;
			case WEST -> OPEN_WEST;
			case EAST -> OPEN_EAST;
			default -> OPEN_NORTH;
			};
		}

		return switch (state.getValue(FACING)) {
		case NORTH -> CLOSED_NORTH;
		case SOUTH -> CLOSED_SOUTH;
		case WEST -> CLOSED_WEST;
		case EAST -> CLOSED_EAST;
		default -> CLOSED_NORTH;
		};
	}

	@Override
	public void appendHoverText(ItemStack stack, BlockGetter level, List<Component> component, TooltipFlag flag) {
		if (!Screen.hasShiftDown()) {
			component.add(Component.translatable("tooltip.beautify.shift").withStyle(ChatFormatting.YELLOW));
		}

		if (Screen.hasShiftDown()) {
			component.add(Component.translatable("tooltip.beautify.blinds.1").withStyle(ChatFormatting.GRAY));
			component.add(Component.translatable("tooltip.beautify.blinds.2").withStyle(ChatFormatting.GRAY));
		}
		super.appendHoverText(stack, level, component, flag);
	}
}
