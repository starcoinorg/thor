export interface GameEngine {

  init(): void;

  update(player: number, state: Int8Array): boolean;

  loadState(fullState: Int8Array): void;


  getState(): Int8Array;

  isGameOver(): boolean;

  getWinner(): number;

}
