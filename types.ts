
export interface Source {
  uri: string;
  title: string;
}

export interface Message {
  id: string;
  text: string;
  sender: 'user' | 'ai';
  sources?: Source[];
}
    